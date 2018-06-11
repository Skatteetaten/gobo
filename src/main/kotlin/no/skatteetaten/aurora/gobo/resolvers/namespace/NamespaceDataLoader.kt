package no.skatteetaten.aurora.gobo.resolvers.namespace

import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.application.Application
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdge
import org.springframework.stereotype.Component

@Component
class NamespaceDataLoader(
    val applicationService: ApplicationService
) : KeysDataLoader<Application, Namespace2> {
    override fun getByKeys(keys: List<Application>): List<Namespace2> {
        val affiliationIds = keys.map { it.affiliationId }.distinct()
        val allEdges = applicationService
            .getApplications(affiliationIds)
            .map { createApplicationEdge(it) }

        return keys.map { application ->
            val edges = allEdges.filter { it.node.namespaceId == application.namespaceId }
            Namespace2(
                application.namespaceId,
                application.affiliationId,
                ApplicationsConnection(edges, null)
            )
        }
    }
}