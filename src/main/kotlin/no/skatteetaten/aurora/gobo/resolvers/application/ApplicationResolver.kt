package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.ImageRepo
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class ApplicationResolver(val tagDataLoader: NoCacheBatchDataLoader<ImageRepo, List<String>>) :
    GraphQLResolver<Application> {

    fun tags(application: Application): CompletableFuture<List<String>> {

        val imageRepo = application.applicationInstances.firstOrNull()?.details?.imageDetails?.dockerImageRepo
            ?: return CompletableFuture.completedFuture(emptyList())

        return tagDataLoader.load(imageRepo.let { ImageRepo.fromRepoString(it) })
    }
}