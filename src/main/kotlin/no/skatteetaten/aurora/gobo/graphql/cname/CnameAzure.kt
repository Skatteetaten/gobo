package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore

data class CnameAzure(
    val canonicalName: String,
    val ttlInSeconds: Int,
    val namespace: String,
    val clusterId: String,
    val ownerObjectName: String,
) {
    @GraphQLIgnore
    fun containsAffiliation(affiliations: List<String>) = affiliations.any { namespace.substringBefore("-") == it }
}
