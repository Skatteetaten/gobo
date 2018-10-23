package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
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
        applicationService.getApplications(affiliations, applications).wait()

    fun getApplication(id: String): ApplicationResource =
        applicationService.getApplication(id).wait()

    fun getApplicationDeployment(applicationDeploymentId: String) =
        applicationService.getApplicationDeployment(applicationDeploymentId).wait()

    fun getApplicationDeploymentDetails(token: String, applicationDeploymentId: String) =
        applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId).wait()

    fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams) =
        applicationService.refreshApplicationDeployment(token, refreshParams).blockAndHandleError()

    private fun <T> Mono<T>.wait() = this.blockNonNullAndHandleError(Duration.ofSeconds(30))
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
        return resources.map { if (applications == null) it else it.filter { applications.contains(it.name) } }
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

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply { addAll("affiliation", affiliations) }

    fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams) =
        webClient
            .post()
            .uri("/api/auth/refresh")
            .body(BodyInserters.fromObject(refreshParams))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .bodyToMono<Void>()
}