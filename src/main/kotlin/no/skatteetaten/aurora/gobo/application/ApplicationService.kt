package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.skatteetaten.aurora.gobo.user.UserService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class ApplicationService(val webClient: WebClient, val objectMapper: ObjectMapper, val userService: UserService) {

    fun getApplications(affiliations: List<String>): List<ApplicationResource> {
        val response = webClient
            .get()
            .uri {
                it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
            }
            .retrieve()
            .bodyToMono<String>()
            .block() ?: return emptyList()

        return objectMapper.readValue(response)
    }

    fun getApplicationInstanceDetails(affiliations: List<String>): List<ApplicationInstanceDetailsResource> =
        affiliations.flatMap { getApplicationInstanceDetails(it) }

    private fun getApplicationInstanceDetails(affiliation: String): List<ApplicationInstanceDetailsResource> {
        val response = webClient
            .get()
            .uri("/api/applicationinstancedetails?affiliation={affiliation}", affiliation)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${userService.getToken()}")
            .retrieve()
            .bodyToMono<String>()
            .onErrorResume { Mono.empty() }
            .block() ?: return emptyList()
        return objectMapper.readValue(response)
    }

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply { addAll("affiliation", affiliations) }
}