package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import no.skatteetaten.aurora.gobo.resolvers.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class ImageTagDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : MultipleKeysDataLoader<ImageTag, Image?> {
    override fun getByKeys(user: User, keys: MutableSet<ImageTag>): Map<ImageTag, Try<Image?>> {

        val imageReposAndTags = ImageRepoAndTags.fromImageTags(keys)

        return try {
            val auroraResponse =
                imageRegistryServiceBlocking.findTagsByName(
                    imageReposAndTags = imageReposAndTags,
                    token = user.token
                )

            val successes = auroraResponse.items.associate { imageTagResource ->
                val imageTag = ImageTag.fromTagString(imageTagResource.requestUrl, "/")
                val image = Image(imageTagResource.timeline.buildEnded, imageTagResource.requestUrl)
                imageTag to Try.succeeded(image)
            }

            val failures = auroraResponse.failure.associate {
                val imageTag = ImageTag.fromTagString(it.url, "/")

                imageTag to Try.failed<Image?>(
                    SourceSystemException(message = it.errorMessage, sourceSystem = "cantus")
                )
            }

            successes + failures
        } catch (e: SourceSystemException) {
            keys.associate { it to Try.failed<Image>(e) }
        }
    }
}