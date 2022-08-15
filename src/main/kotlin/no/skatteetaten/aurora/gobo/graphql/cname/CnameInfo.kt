package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore

data class CnameEntry(val cname: String, val host: String, val ttl: Int)

data class CnameInfo(
    val status: String,
    val clusterId: String,
    val appName: String,
    val namespace: String,
    val routeName: String,
    val message: String,
    val entry: CnameEntry
) {
    @GraphQLIgnore
    fun containsAffiliation(affiliations: List<String>) = affiliations.any { namespace.substringBefore("-") == it }
}
