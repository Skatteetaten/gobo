package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.springframework.stereotype.Component

@Component
class ApplicationDataLoader(
    val applicationService: ApplicationService
) : KeysDataLoader<String, Affiliation> {
    override fun getByKeys(keys: List<String>): List<Affiliation> {
        val allEdges = applicationService.getApplications(keys.distinct()).map {
            ApplicationEdge(
                Application(
                    it.affiliation,
                    it.environment,
                    it.name,
                    it.name,
                    Status(it.status.code, it.status.comment),
                    Version(it.version.deployTag, it.version.auroraVersion)
                )
            )
        }

        return keys.map { key ->
            val edges = allEdges.filter { it.node.affiliationId == key }
            Affiliation(key, ApplicationsConnection(edges, null))
        }
    }
}
