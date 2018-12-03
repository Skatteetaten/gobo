package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component
import java.time.Instant

data class ImageDetails(
    val imageBuildTime: Instant?,
    val dockerImageReference: String?,
    val dockerImageTagReference: String?
)

@Component
class ImageDetailsResolver : GraphQLResolver<ImageDetails> {

    fun latestDockerImageTagReference(imageDetails: ImageDetails, dfe: DataFetchingEnvironment) =
        imageDetails.dockerImageTagReference?.let {
            dfe.loader(ImageTagDigestDataLoader::class).load(ImageTag.fromTagString(it))
        }
}

@Component
class ImageTagDigestDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : KeyDataLoader<ImageTag, String> {
    override fun getByKey(user: User, key: ImageTag): Try<String> {
        return Try.tryCall { imageRegistryServiceBlocking.resolveTagToSh(key) }
    }
}