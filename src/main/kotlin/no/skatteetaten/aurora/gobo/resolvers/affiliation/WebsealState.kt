package no.skatteetaten.aurora.gobo.resolvers.affiliation

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
            if (propertyNames == null) {
                junction
            } else {
                junction.filter { propertyNames.contains(it.key) }
            }
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