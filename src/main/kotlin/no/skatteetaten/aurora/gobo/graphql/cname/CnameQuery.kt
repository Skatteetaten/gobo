package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService
import org.springframework.stereotype.Component

@Component
class CnameQuery(
    val cnameService: CnameService,
    val spotlessService: SpotlessCnameService
) : Query {

    @GraphQLDescription("Usage: { cname(type: \"<type>\") { ... on <implementation> { <fields> } } }")
    suspend fun cname(
        @GraphQLDescription("type indicates the type of cname to return. Supported values: onPrem, azure")
        type: String,
        affiliations: List<String>? = null
    ): List<Cname> {
        val cnameList = when (type.lowercase()) {
            "onprem" -> cnameService.getCnameInfo()
            "azure" -> spotlessService.getCnameContent()
            else -> emptyList()
        }
        return affiliations?.let { cnameList.filter { it.containsAffiliation(affiliations) } } ?: cnameList
    }
}

interface Cname {
    val clusterId: String
    val namespace: String

    @GraphQLIgnore
    fun containsAffiliation(affiliations: List<String>) = affiliations.any { namespace.substringBefore("-") == it }
}

data class CnameEntry(val cname: String, val host: String, val ttl: Int)

@GraphQLDescription("Object returned when argument `type`=`onPrem`")
data class CnameInfo(
    override val clusterId: String,
    override val namespace: String,
    val status: String,
    val appName: String,
    val routeName: String,
    val message: String,
    val entry: CnameEntry
) : Cname

@GraphQLDescription("Object returned when argument `type`=`azure`")
data class CnameAzure(
    override val clusterId: String,
    override val namespace: String,
    val canonicalName: String,
    val ttlInSeconds: Int,
    val ownerObjectName: String
) : Cname
