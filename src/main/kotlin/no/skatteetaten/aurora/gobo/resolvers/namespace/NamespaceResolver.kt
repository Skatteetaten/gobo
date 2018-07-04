package no.skatteetaten.aurora.gobo.resolvers.namespace

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class NamespaceResolver(
    private val affiliationDataLoader: NoCacheBatchDataLoader<String, Affiliation>
) : GraphQLResolver<Namespace> {

    fun affiliation(namespace: Namespace): CompletableFuture<Affiliation> =
        affiliationDataLoader.load(namespace.affiliationId)
}