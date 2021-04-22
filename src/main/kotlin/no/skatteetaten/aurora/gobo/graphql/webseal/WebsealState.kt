package no.skatteetaten.aurora.gobo.graphql.webseal

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.integration.skap.WebsealStateResource

data class Acl(
    val aclName: String,
    val anyOther: Boolean,
    val `open`: Boolean,
    val roles: List<String>
)

data class WebsealState(
    val acl: Acl,
    @GraphQLIgnore
    val junctions: List<Map<String, String>>,
    val name: String,
    val namespace: String,
    val routeName: String
) {

    fun junctions(propertyNames: List<String>? = null) = junctions.map { junction ->
        if (propertyNames == null) {
            junction
        } else {
            junction.filter { propertyNames.contains(it.key) }
        }
    }.map {
        jacksonObjectMapper().writeValueAsString(it)
    }

    companion object {

        fun create(resource: WebsealStateResource): WebsealState {
            return WebsealState(
                acl = resource.acl,
                junctions = resource.junctions,
                name = resource.name,
                namespace = resource.namespace,
                routeName = resource.routeName
            )
        }
    }
}
