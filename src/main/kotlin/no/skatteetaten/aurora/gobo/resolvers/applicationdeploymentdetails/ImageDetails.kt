package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

data class ImageDetails(
    val imageBuildTime: Instant?,
    val digest: String?,
    val dockerImageTagReference: String?
)

data class ImageTagDigestDTO(val imageTag: ImageTag, val expecedDigest: String?)
@Component
class ImageDetailsResolver : GraphQLResolver<ImageDetails> {

    private val logger = LoggerFactory.getLogger(ImageDetailsResolver::class.java)

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
                val imageRepoDto = key.imageTag.imageRepository.toImageRepo(key.imageTag.name)
                imageRegistryServiceBlocking.resolveTagToSha(imageRepoDto) == key.expecedDigest
            }
        }
    }
}