package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.integration.skap.Acl
import no.skatteetaten.aurora.gobo.integration.skap.WebsealStateResource

data class WebsealState(
    val acl: Acl,
    val junctions: List<Map<String, String>>,
    val name: String,
    val namespace: String,
    val routeName: String
) {

    fun junctions(propertyNames: List<String>?) =
        junctions.map { junction ->
            val filteredJunction = if (propertyNames == null) {
                junction
            } else {
                junction.filter { propertyNames.contains(it.key) }
            }

            filteredJunction.map { jacksonObjectMapper().writeValueAsString(it) }
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