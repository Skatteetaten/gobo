package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class ApplicationServiceBlocking(private val applicationService: ApplicationService) {
    fun getApplications(affiliations: List<String>, applications: List<String>? = null): List<ApplicationResource> {
        val resources =
            applicationService.getApplications(affiliations, applications).blockAndHandleError()
                ?: return emptyList()
        return if (applications == null) resources else resources.filter { applications.contains(it.name) }
    }

    fun getApplication(id: String): ApplicationResource =
        applicationService.getApplication(id).blockNonNullAndHandleError()

    fun getApplicationDeployment(applicationDeploymentId: String) =
        applicationService.getApplicationDeployment(applicationDeploymentId).blockNonNullAndHandleError()

    fun getApplicationDeploymentDetails(
        token: String,
        applicationDeploymentId: String
    ): ApplicationDeploymentDetailsResource =
        applicationService.getApplicationDeploymentDetails(
            token,
            applicationDeploymentId
        ).blockNonNullAndHandleError()

    fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams) =
        applicationService.refreshApplicationDeployment(token, refreshParams).block()
}

@Service
class ApplicationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): Mono<List<ApplicationResource>> {
        return webClient
            .get()
            .uri {
                it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
            }
            .retrieve()
            .bodyToMono()
    }

    fun getApplication(id: String): Mono<ApplicationResource> {
        return webClient
            .get()
            .uri("/api/application/{id}", id)
            .retrieve()
            .onStatus(HttpStatus::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().defaultIfEmpty("").map { body ->
                    SourceSystemException(
                        message = "Failed to get application, status:${clientResponse.statusCode().value()} message:${clientResponse.statusCode().reasonPhrase}",
                        code = clientResponse.statusCode().value().toString(),
                        errorMessage = body
                    )
                }
            }.bodyToMono()
    }

    fun getApplicationDeployment(applicationDeploymentId: String): Mono<ApplicationDeploymentResource> {
        return webClient
            .get()
            .uri("/api/applicationdeployment/{applicationDeploymentId}", applicationDeploymentId)
            .retrieve()
            .onStatus(HttpStatus::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().defaultIfEmpty("").map { body ->
                    SourceSystemException(
                        message = "Failed to get application deployment, status:${clientResponse.statusCode().value()} message:${clientResponse.statusCode().reasonPhrase}",
                        code = clientResponse.statusCode().value().toString(),
                        errorMessage = body
                    )
                }
            }.bodyToMono()
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
            .onStatus(HttpStatus::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().defaultIfEmpty("").map { body ->
                    SourceSystemException(
                        message = "Failed to get application deployment details, status:${clientResponse.statusCode().value()} message:${clientResponse.statusCode().reasonPhrase}",
                        code = clientResponse.statusCode().value().toString(),
                        errorMessage = body
                    )
                }
            }.bodyToMono()
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
            .onStatus(HttpStatus::isError) { clientResponse ->
                clientResponse.bodyToMono<String>().defaultIfEmpty("").map { body ->
                    SourceSystemException(
                        message = "Failed to refresh, status:${clientResponse.statusCode().value()} message:${clientResponse.statusCode().reasonPhrase}",
                        code = clientResponse.statusCode().value().toString(),
                        errorMessage = body
                    )
                }
            }.bodyToMono<Void>()
}