package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class ApplicationDataLoader(
    val applicationService: ApplicationService
) : DataLoader<String, Affiliation>(BatchLoader { keys: List<String> ->
    CompletableFuture.supplyAsync({
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

        keys.map { key ->
            val edges = allEdges.filter { it.node.affiliation == key }
            Affiliation(key, ApplicationsConnection(edges, null))
        }
    })
}, DataLoaderOptions.newOptions().setCachingEnabled(false))