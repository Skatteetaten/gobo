package no.skatteetaten.aurora.gobo.integration.mokey

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.skatteetaten.aurora.gobo.exceptions.RestResponseException
import no.skatteetaten.aurora.gobo.security.UserService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class ApplicationService(val webClient: WebClient, val objectMapper: ObjectMapper, val userService: UserService) {

    fun getApplications(affiliations: List<String>, applications: List<String>? = null): List<ApplicationResource> {
        try {
            val response = webClient
                    .get()
                    .uri {
                        it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
                    }
                    .retrieve()
                    .bodyToMono<String>()
                    .block() ?: return emptyList()

            val applicationResources = objectMapper.readValue<List<ApplicationResource>>(response)
            return if (applications == null) applicationResources else applicationResources.filter { applications.contains(it.name) }
        } catch (e: WebClientResponseException) {
            throw RestResponseException("Failed to get application, status:${e.statusCode} message:${e.statusText}", e, e.statusText, "Failed to get applications")
        }
    }

    fun getApplicationDeploymentDetails(affiliations: List<String>): List<ApplicationDeploymentDetailsResource> =
            affiliations.flatMap { getApplicationDeploymentDetails(it) }

    private fun getApplicationDeploymentDetails(affiliation: String): List<ApplicationDeploymentDetailsResource> {
        try {
            val response = webClient
                    .get()
                    .uri("/api/applicationdeploymentdetails?affiliation={affiliation}", affiliation)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userService.getToken()}")
                    .retrieve()
                    .bodyToMono<String>()
                    .block() ?: return emptyList()
            return objectMapper.readValue(response)
        } catch (e: WebClientResponseException) {
            throw RestResponseException("Failed to get application deployment details, status:${e.statusCode} message:${e.statusText}", e, e.statusText, "Failed to get application deployment details")
        }
    }

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> =
            LinkedMultiValueMap<String, String>().apply { addAll("affiliation", affiliations) }
}