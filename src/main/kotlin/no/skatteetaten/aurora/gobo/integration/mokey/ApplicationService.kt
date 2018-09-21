package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.security.UserService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class ApplicationService(val webClient: WebClient, val userService: UserService) {

    fun getApplications(affiliations: List<String>, applications: List<String>? = null): List<ApplicationResource> {
        try {
            val resources = webClient
                    .get()
                    .uri {
                        it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
                    }
                    .retrieve()
                    .bodyToMono<List<ApplicationResource>>()
                    .block() ?: return emptyList()

            return if (applications == null) resources else resources.filter { applications.contains(it.name) }
        } catch (e: WebClientResponseException) {
            throw SourceSystemException("Failed to get application, status:${e.statusCode} message:${e.statusText}", e, e.statusText, "Failed to get applications")
        }
    }

    fun getApplicationDeploymentDetails(affiliations: List<String>): List<ApplicationDeploymentDetailsResource> =
            affiliations.flatMap { getApplicationDeploymentDetailsForAffiliation(it) }

    fun getApplicationDeploymentDetails(applicationDeploymentId: String): Mono<ApplicationDeploymentDetailsResource> {
        return try {
            webClient
                .get()
                .uri("/api/applicationdeploymentdetails/{applicationDeploymentId}", applicationDeploymentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${userService.getToken()}")
                .retrieve()
                .bodyToMono()
        } catch (e: WebClientResponseException) {
            Mono.error(SourceSystemException("Failed to get application deployment details, status:${e.statusCode} message:${e.statusText}", e, e.statusText, "Failed to get application deployment details"))
        }
    }

    private fun getApplicationDeploymentDetailsForAffiliation(affiliation: String): List<ApplicationDeploymentDetailsResource> {
        try {
            return webClient
                    .get()
                    .uri("/api/applicationdeploymentdetails?affiliation={affiliation}", affiliation)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userService.getToken()}")
                    .retrieve()
                    .bodyToMono<List<ApplicationDeploymentDetailsResource>>()
                    .block() ?: return emptyList()
        } catch (e: WebClientResponseException) {
            throw SourceSystemException("Failed to get application deployment details, status:${e.statusCode} message:${e.statusText}", e, e.statusText, "Failed to get application deployment details")
        }
    }

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> =
            LinkedMultiValueMap<String, String>().apply { addAll("affiliation", affiliations) }

    fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams): Mono<Void> {
        return try {
            webClient
                .post()
                .uri("/refresh")
                .body(BodyInserters.fromObject(refreshParams))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .bodyToMono()
        } catch (e: WebClientResponseException) {
            Mono.error(SourceSystemException("Failed to get application deployment details, status:${e.statusCode} message:${e.statusText}", e, e.statusText, "Failed to get application deployment details"))
        }
    }
}