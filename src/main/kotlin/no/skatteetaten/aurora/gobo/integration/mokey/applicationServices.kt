package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ApplicationServiceBlocking(private val applicationService: ApplicationService) {
    fun getApplications(affiliations: List<String>, applications: List<String>? = null) =
        applicationService.getApplications(affiliations, applications).blockNonNullWithTimeout()

    fun getApplication(id: String): ApplicationResource =
        applicationService.getApplication(id).blockNonNullWithTimeout()

    fun getApplicationDeployment(applicationDeploymentId: String) =
        applicationService.getApplicationDeployment(applicationDeploymentId).blockNonNullWithTimeout()

    fun getApplicationDeployment(applicationDeploymentRefs: List<ApplicationDeploymentRef>) =
        applicationService.getApplicationDeployment(applicationDeploymentRefs).blockNonNullWithTimeout()

    fun getApplicationDeploymentDetails(token: String, applicationDeploymentId: String) =
        applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId).blockNonNullWithTimeout()

    fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams) =
        applicationService.refreshApplicationDeployment(token, refreshParams).blockWithTimeout()

    fun getApplicationDeploymentsForDatabases(token: String, databaseIds: List<String>) =
        applicationService.getApplicationDeploymentsForDatabases(token, databaseIds).blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockNonNullWithTimeout() = this.blockNonNullAndHandleError(Duration.ofSeconds(30), "mokey")
    private fun <T> Mono<T>.blockWithTimeout() = this.blockAndHandleError(Duration.ofSeconds(30), "mokey")
}

@Service
class ApplicationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): Mono<List<ApplicationResource>> {
        val resources: Mono<List<ApplicationResource>> = webClient
            .get()
            .uri {
                it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
            }
            .retrieve()
            .bodyToMono()
        return resources.map { resource ->
            if (applications == null) resource else resource.filter {
                applications.contains(it.name)
            }
        }
    }

    fun getApplication(id: String): Mono<ApplicationResource> {
        return webClient
            .get()
            .uri("/api/application/{id}", id)
            .retrieve()
            .bodyToMono()
    }

    fun getApplicationDeployment(applicationDeploymentId: String): Mono<ApplicationDeploymentResource> {
        return webClient
            .get()
            .uri("/api/applicationdeployment/{applicationDeploymentId}", applicationDeploymentId)
            .retrieve()
            .bodyToMono()
    }

    fun getApplicationDeployment(applicationDeploymentRefs: List<ApplicationDeploymentRef>) : Mono<List<ApplicationDeploymentResource>> {
        return webClient
            .post()
            .uri("/api/applicationdeployment")
            .body(BodyInserters.fromValue(applicationDeploymentRefs))
            .retrieve()
            .bodyToMono()
    }
    fun getApplicationDeploymentDetails(
        token: String,
        applicationDeploymentId: String
    ): Mono<ApplicationDeploymentDetailsResource> {
        return webClient
            .get()
            .uri("/api/auth/applicationdeploymentdetails/{applicationDeploymentId}", applicationDeploymentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .bodyToMono()
    }

    fun getApplicationDeploymentsForDatabases(
        token: String,
        databaseIds: List<String>
    ): Mono<List<ApplicationDeploymentWithDbResource>> =
        webClient
            .post()
            .uri("/api/auth/applicationdeploymentbyresource/databases")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .body(BodyInserters.fromValue(databaseIds))
            .retrieve()
            .bodyToMono()

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply { addAll("affiliation", affiliations) }

    fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams) =
        webClient
            .post()
            .uri("/api/auth/refresh")
            .body(BodyInserters.fromValue(refreshParams))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .bodyToMono<Void>()
}
