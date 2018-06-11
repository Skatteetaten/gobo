package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace2
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class ApplicationResolver(
    val affiliationDataLoader: NoCacheBatchDataLoader<String, Affiliation>,
    val namespaceDataLoader: NoCacheBatchDataLoader<Application, Namespace2>
) : GraphQLResolver<Application> {

    fun namespace(application: Application): CompletableFuture<Namespace2> =
        namespaceDataLoader.load(application)

    fun affiliation(application: Application): CompletableFuture<Affiliation> =
        affiliationDataLoader.load(application.affiliationId)
}