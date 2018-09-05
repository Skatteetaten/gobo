package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import org.dataloader.Try
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Component
class ImageRepositoryTagResolver(private val tagDataLoader: NoCacheBatchDataLoader<ImageTag, Try<Instant>>) :
        GraphQLResolver<ImageTag> {

    fun lastModified(imageTag: ImageTag): CompletableFuture<Try<Instant>>? = tagDataLoader.load(imageTag)
}