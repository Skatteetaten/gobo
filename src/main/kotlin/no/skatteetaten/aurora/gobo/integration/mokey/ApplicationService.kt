package no.skatteetaten.aurora.gobo.integration.mokey

import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.ApplicationRedeployException
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.integration.awaitWithRetry
import no.skatteetaten.aurora.gobo.integration.boober.RedeployResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Service
class ApplicationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    suspend fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): List<ApplicationResource> {
        val resources: List<ApplicationResource> = webClient
            .get()
            .uri {
                it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
            }
            .retrieve()
            .handleHttpStatusErrors()
            .awaitWithRetry()
        return applications?.let { resources.filter { applications.contains(it.name) } } ?: resources
    }

    suspend fun getApplication(id: String): ApplicationResource {
        return webClient
            .get()
            .uri("/api/application/{id}", id)
            .retrieve()
            .handleHttpStatusErrors()
            .awaitWithRetry()
    }

    suspend fun getApplicationDeployment(applicationDeploymentId: String): ApplicationDeploymentResource {
        return webClient
            .get()
            .uri("/api/applicationdeployment/{applicationDeploymentId}", applicationDeploymentId)
            .retrieve()
            .handleHttpStatusErrors()
            .awaitWithRetry()
    }

    suspend fun getApplicationDeployment(applicationDeploymentRefs: List<ApplicationDeploymentRef>): List<ApplicationDeploymentResource> {
        return webClient
            .post()
            .uri("/api/applicationdeployment")
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
            .handleHttpStatusErrors()
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
            .handleHttpStatusErrors()
            .awaitWithRetry()

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply { addAll("affiliation", affiliations) }

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

    private fun WebClient.ResponseSpec.handleHttpStatusErrors() =
        onStatus({ it != HttpStatus.OK }) {
            Mono.error(
                MokeyIntegrationException(
                    "Downstream request failed with ${it.statusCode().reasonPhrase}",
                    it.statusCode()
                )
            )
        }
}
