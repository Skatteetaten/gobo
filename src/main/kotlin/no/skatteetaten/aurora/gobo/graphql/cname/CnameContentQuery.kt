package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService
import org.springframework.stereotype.Component

data class CnameContent(
    val canonicalName: String,
    val ttlInSeconds: Int,
    val namespace: String,
    val clusterId: String,
    val ownerObjectName: String
) {
    @GraphQLIgnore
    fun containsAffiliation(affiliations: List<String>) = affiliations.any { namespace.substringBefore("-") == it }
}

@Component
class CnameContentQuery(val spotlessCnameService: SpotlessCnameService) : Query {
    suspend fun cnameContent(affiliations: List<String>? = null): List<CnameContent> {
        val cnameContent = spotlessCnameService.getCnameContent()
        return affiliations?.let { cnameContent.filter { it.containsAffiliation(affiliations) } } ?: cnameContent
    }
}
