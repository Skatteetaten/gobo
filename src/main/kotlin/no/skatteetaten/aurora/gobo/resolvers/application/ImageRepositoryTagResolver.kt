package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Component
class ImageRepositoryTagResolver(private val tagDataLoader: NoCacheBatchDataLoader<ImageTag, Instant>) :
    GraphQLResolver<ImageTag> {

    fun lastModified(imageTag: ImageTag): CompletableFuture<Instant> = tagDataLoader.load(imageTag)
}