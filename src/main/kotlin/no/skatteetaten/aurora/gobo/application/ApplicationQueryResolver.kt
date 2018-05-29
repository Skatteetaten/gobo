package no.skatteetaten.aurora.gobo.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.util.UriComponentsBuilder

@Component
class ApplicationQueryResolver(
    @Value("\${mokey.url}") val mokeyUrl: String,
    val restTemplate: RestTemplate,
    val objectMapper: ObjectMapper
) : GraphQLQueryResolver {

    fun getApplications(affiliations: List<String>): List<Application> {
        val response = restTemplate.getForObject<String>(getUrl(affiliations)) ?: return emptyList()
        return objectMapper.readValue(response)
    }

    private fun getUrl(affiliations: List<String>): String {
        val parameters = LinkedMultiValueMap<String, String>().apply {
            addAll("affiliation", affiliations)
        }
        val builder = UriComponentsBuilder
            .fromUriString("$mokeyUrl/api/application")
            .queryParams(parameters)
        return builder.toUriString()
    }
}