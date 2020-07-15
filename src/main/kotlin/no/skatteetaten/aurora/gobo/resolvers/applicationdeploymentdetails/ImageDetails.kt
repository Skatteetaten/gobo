package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.MyGraphQLContext
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.load
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
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
class ImageDetailsResolver : Query {

    fun isFullyQualified(imageDetails: ImageDetails, dfe: DataFetchingEnvironment) =
        imageDetails.dockerImageTagReference?.let {
            ImageTag.fromTagString(it).imageRepository.registryUrl != null
        } ?: false

    suspend fun isLatestDigest(imageDetails: ImageDetails, dfe: DataFetchingEnvironment) =
        imageDetails.dockerImageTagReference?.let {
            logger.debug("Loading docker image tag reference for tag=$it")
            dfe.load<ImageTagDigestDTO, ImageTagIsLatestDigestDataLoader>(
                ImageTagDigestDTO(
                    ImageTag.fromTagString(it),
                    imageDetails.digest
                )
            )
        }

    @Component
    class ImageTagIsLatestDigestDataLoader(
        val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
    ) : KeyDataLoader<ImageTagDigestDTO, Boolean> {

        override suspend fun getByKey(key: ImageTagDigestDTO, ctx: MyGraphQLContext): Boolean {
            if (key.imageTag.imageRepository.registryUrl == null) {
                return false
            }

            val imageRepoDto = key.imageTag.imageRepository.toImageRepo()
            return imageRegistryServiceBlocking.resolveTagToSha(
                imageRepoDto,
                key.imageTag.name,
                "user.token"
            ) == key.expecedDigest
        }
    }
}
