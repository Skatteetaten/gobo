package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder

@Service
class ApplicationService(val webClient: WebClient, val objectMapper: ObjectMapper) {

    fun getApplications(affiliations: List<String>): List<ApplicationResource> {
        val path = buildPath(affiliations)
        val response = webClient
            .get()
            .uri(path)
            .retrieve()
            .bodyToMono<String>()
            .block() ?: return emptyList()

        return objectMapper.readValue(response)
    }

    private fun buildPath(affiliations: List<String>): String {
        val parameters = LinkedMultiValueMap<String, String>().apply {
            addAll("affiliation", affiliations)
        }
        val builder = UriComponentsBuilder
            .fromUriString("/api/application")
            .queryParams(parameters)
        return builder.toUriString()
    }
}