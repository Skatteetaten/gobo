package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdge
import org.springframework.stereotype.Component

@Component
class AffiliationDataLoader(
    val applicationService: ApplicationService
) : KeysDataLoader<String, Affiliation> {
    override fun getByKeys(keys: List<String>): List<Affiliation> {
        val allEdges = applicationService
            .getApplications(keys.distinct())
            .map { createApplicationEdge(it) }

        return keys.map { key ->
            val edges = allEdges.filter { it.node.affiliationId == key }
            Affiliation(key, ApplicationsConnection(edges, null))
        }
    }
}
