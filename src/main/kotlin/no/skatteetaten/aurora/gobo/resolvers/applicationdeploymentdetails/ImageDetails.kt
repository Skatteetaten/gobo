package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagDto
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component

data class ImageDetails(
    val digest: String?,
    val dockerImageTagReference: String?
)

private val logger = KotlinLogging.logger {}

@Component
class ImageDetailsResolver : GraphQLResolver<ImageDetails> {

    fun isLatestDigest(imageDetails: ImageDetails, dfe: DataFetchingEnvironment) =
        imageDetails.dockerImageTagReference?.let { tag ->
            logger.debug("Loading docker image tag reference for tag=$tag")
            dfe.loader(ImageTagIsLatestDigestDataLoader::class)
                .load(ImageTag.fromTagString(tag)).thenApply {
                    it.dockerDigest == imageDetails.digest
                }
        }

    fun imageBuildTime(imageDetails: ImageDetails, dfe: DataFetchingEnvironment) =
        imageDetails.dockerImageTagReference?.let { tag ->
            logger.debug("Loading docker image tag reference for tag=$tag")
            dfe.loader(ImageTagIsLatestDigestDataLoader::class)
                .load(ImageTag.fromTagString(tag)).thenApply {
                    it.created
                }
        }
}

@Component
class ImageTagIsLatestDigestDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : KeyDataLoader<ImageTag, ImageTagDto> {
    override fun getByKey(user: User, key: ImageTag): Try<ImageTagDto> {
        return Try.tryCall {
            val imageRepoDto = key.imageRepository.toImageRepo()
            imageRegistryServiceBlocking.resolveTagToSha(
                imageRepoDto,
                key.name,
                user.token
            )
        }
    }
}
