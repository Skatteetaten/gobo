package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class ApplicationService(val webClient: WebClient, val objectMapper: ObjectMapper) {

    fun getApplications(affiliations: List<String>): List<ApplicationResource> {
        val response = webClient
            .get()
            .uri({
                it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
            })
            .retrieve()
            .bodyToMono<String>()
            .block() ?: return emptyList()

        return objectMapper.readValue(response)
    }

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> {
        val params = LinkedMultiValueMap<String, String>().apply {
            addAll("affiliation", affiliations)
        }
        return params
    }
}