package no.skatteetaten.aurora.gobo.integration.mokey

import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.ApplicationRedeployException
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.integration.awaitWithRetry
import no.skatteetaten.aurora.gobo.integration.boober.RedeployResponse
import no.skatteetaten.aurora.gobo.integration.onStatusNotFound
import no.skatteetaten.aurora.gobo.integration.onStatusNotOk
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

private val logger = KotlinLogging.logger {}

@Service
class ApplicationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    suspend fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): List<ApplicationResource> {

        val resources: List<ApplicationResource> = webClient
            .get()
            .uri("/api/application?affiliation={affiliations}", affiliations.joinToString())
            .retrieve()
            .handleHttpStatusErrors(affiliations.toString())
            .awaitWithRetry()
        return applications?.let { resources.filter { applications.contains(it.name) } } ?: resources
    }

    suspend fun getApplication(id: String): ApplicationResource {
        return webClient
            .get()
            .uri("/api/application/{id}", id)
            .retrieve()
            .handleHttpStatusErrors(id)
            .awaitWithRetry()
    }

    suspend fun getApplicationDeployment(applicationDeploymentId: String): ApplicationDeploymentResource {
        return webClient
            .get()
            .uri("/api/applicationdeployment/{applicationDeploymentId}", applicationDeploymentId)
            .retrieve()
            .handleHttpStatusErrors(applicationDeploymentId)
            .awaitWithRetry()
    }

    suspend fun getApplicationDeployments(
        applicationDeploymentRefs: List<ApplicationDeploymentRef>,
        cached: Boolean = true,
    ): List<ApplicationDeploymentResource> {
        return webClient
            .post()
            .uri {
                it
                    .path("/api/applicationdeployment")
                    .queryParam("cached", cached)
                    .build()
            }
            .body(BodyInserters.fromValue(applicationDeploymentRefs))
            .retrieve()
            .handleHttpStatusErrors()
            .awaitWithRetry()
    }

    suspend fun getApplicationDeploymentDetails(
        token: String,
        applicationDeploymentId: String
    ): ApplicationDeploymentDetailsResource {
        return webClient
            .get()
            .uri("/api/auth/applicationdeploymentdetails/{applicationDeploymentId}", applicationDeploymentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .handleHttpStatusErrors(applicationDeploymentId)
            .awaitWithRetry()
    }

    suspend fun getApplicationDeploymentsForDatabases(
        token: String,
        databaseIds: List<String>
    ): List<ApplicationDeploymentWithDbResource> =
        webClient
            .post()
            .uri("/api/auth/applicationdeploymentbyresource/databases")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .body(BodyInserters.fromValue(databaseIds))
            .retrieve()
            .handleHttpStatusErrors(databaseIds.toString())
            .awaitWithRetry()

    suspend fun refreshApplicationDeployment(
        token: String,
        refreshParams: RefreshParams,
        redeployResponse: RedeployResponse? = null
    ) {
        kotlin.runCatching {
            webClient
                .post()
                .uri("/api/auth/refresh")
                .body(BodyInserters.fromValue(refreshParams))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .bodyToMono<Unit>()
                .awaitFirstOrNull()
        }.recover { // Code to handle migrations from old to new id
            if (redeployResponse != null && it is WebClientResponseException && it.statusCode == HttpStatus.BAD_REQUEST) {
                logger.info("Refresh of applicationDeploymentId ${refreshParams.applicationDeploymentId} failed")
                throw ApplicationRedeployException(
                    "Refresh of redeployed application failed",
                    it,
                    "APP_REFRESH_FAILED",
                    redeployResponse
                )
            }

            throw it
        }.getOrThrow()
    }
}
internal fun WebClient.ResponseSpec.handleHttpStatusErrors(id: String? = null) =
    onStatusNotFound { status, body ->
        MokeyIntegrationException(
            message = "The requested resource was not found",
            integrationResponse = id?.let { "id:$it, body: $body" } ?: body,
            status = status
        )
    }.onStatusNotOk { status, body ->
        MokeyIntegrationException(
            message = "Downstream request failed with ${status.reasonPhrase}",
            integrationResponse = id?.let { "id:$it, body: $body" } ?: body,
            status = status
        )
    }
