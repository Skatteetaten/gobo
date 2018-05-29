package no.skatteetaten.aurora.gobo.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@Component
class ApplicationQuery(
    @Value("\${mokey.url}") val mokeyUrl: String,
    val restTemplate: RestTemplate,
    val objectMapper: ObjectMapper
) : GraphQLQueryResolver {

    fun getApplications(): List<Application> {
        val response =
            restTemplate.getForObject<String>(mokeyUrl)
                ?: return emptyList()

        return objectMapper.readValue(response)
    }
}