package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class ImageTagDataLoader(val imageRegistryServiceBlocking: ImageRegistryServiceBlocking) : MultipleKeysDataLoader<ImageTag, Image?> {
    override suspend fun getByKeys(keys: Set<ImageTag>, ctx: GoboGraphQLContext): Map<ImageTag, Try<Image?>> {
        val imageReposAndTags = ImageRepoAndTags.fromImageTags(keys)

        return try {
            val auroraResponse =
                imageRegistryServiceBlocking.findTagsByName(
                    imageReposAndTags = imageReposAndTags,
                    token = ctx.token!!
                )

            val successes = auroraResponse.items.associate { imageTagResource ->
                val imageTag = ImageTag.fromTagString(imageTagResource.requestUrl, "/")
                val image = Image(imageTagResource.timeline.buildEnded, imageTagResource.requestUrl)
                imageTag to Try.succeeded(image)
            }

            val failures = auroraResponse.failure.associate {
                val imageTag = ImageTag.fromTagString(it.url, "/")

                val result: Try<Image?> =
                    if (it.errorMessage.contains("application/vnd.docker.distribution.manifest.v1")) {
                        Try.succeeded(null)
                    } else {
                        Try.failed(
                            SourceSystemException(message = it.errorMessage, sourceSystem = "cantus")
                        )
                    }
                imageTag to result
            }

            successes + failures
        } catch (e: SourceSystemException) {
            keys.associateWith { Try.failed<Image>(e) }
        }
    }
}

/*
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

                val result: Try<Image?> =
                    if (it.errorMessage.contains("application/vnd.docker.distribution.manifest.v1")) {
                        Try.succeeded(null)
                    } else {
                        Try.failed(
                            SourceSystemException(message = it.errorMessage, sourceSystem = "cantus")
                        )
                    }
                imageTag to result
            }

            successes + failures
        } catch (e: SourceSystemException) {
            keys.associate { it to Try.failed<Image>(e) }
        }
    }
}
*/
