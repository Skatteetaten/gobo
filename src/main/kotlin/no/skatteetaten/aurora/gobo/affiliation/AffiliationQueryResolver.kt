package no.skatteetaten.aurora.gobo.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@Component
class AffiliationQueryResolver(
    @Value("\${mokey.url}") val mokeyUrl: String,
    val restTemplate: RestTemplate
) : GraphQLQueryResolver {

    fun getAffiliations(): List<String>? =
        restTemplate.getForObject("$mokeyUrl/api/affiliation")
}