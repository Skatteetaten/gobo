package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
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
    val digest: String?,
    val dockerImageTagReference: String?
)

data class ImageTagDigestDTO(val imageTag: ImageTag, val expecedDigest: String?)

private val logger = KotlinLogging.logger {}

@Component
class ImageDetailsResolver : GraphQLResolver<ImageDetails> {

    fun isLatestDigest(imageDetails: ImageDetails, dfe: DataFetchingEnvironment) =
        imageDetails.dockerImageTagReference?.let {
            logger.debug("Loading docker image tag reference for tag=$it")
            dfe.loader(ImageTagIsLatestDigestDataLoader::class)
                .load(ImageTagDigestDTO(ImageTag.fromTagString(it), imageDetails.digest))
        }

    @Component
    class ImageTagIsLatestDigestDataLoader(
        val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
    ) : KeyDataLoader<ImageTagDigestDTO, Boolean> {
        override fun getByKey(user: User, key: ImageTagDigestDTO): Try<Boolean> {
            return Try.tryCall {
                val imageRepoDto = key.imageTag.imageRepository.toImageRepo()
                imageRegistryServiceBlocking.resolveTagToSha(imageRepoDto, key.imageTag.name) == key.expecedDigest
            }
        }
    }
}