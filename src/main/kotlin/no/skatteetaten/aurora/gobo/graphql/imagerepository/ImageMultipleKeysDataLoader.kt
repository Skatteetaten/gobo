package no.skatteetaten.aurora.gobo.graphql.imagerepository

import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GraphQLExceptionWrapper
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import org.springframework.stereotype.Component

@Component
class ImageMultipleKeysDataLoader(val imageRegistryService: ImageRegistryService) :
    MultipleKeysDataLoader<ImageTag, Image?> {
    override suspend fun getByKeys(keys: Set<ImageTag>, ctx: GoboGraphQLContext): Map<ImageTag, DataFetcherResult<Image?>> {
        val imageReposAndTags = ImageRepoAndTags.fromImageTags(keys)

        return try {
            val auroraResponse =
                imageRegistryService.findTagsByName(
                    imageReposAndTags = imageReposAndTags,
                    token = ctx.token()
                )

            val successes = auroraResponse.items.associate { imageTagResource ->
                val imageTag = ImageTag.fromTagString(imageTagResource.requestUrl, "/")
                val image = Image(imageTagResource.timeline.buildEnded, imageTagResource.requestUrl)
                imageTag to DataFetcherResult.newResult<Image?>().data(image).build()
            }

            val failures = auroraResponse.failure.associate {
                val imageTag = ImageTag.fromTagString(it.url, "/")

                val result = DataFetcherResult.newResult<Image?>().apply {
                    if (it.errorMessage.contains("application/vnd.docker.distribution.manifest.v1")) {
                        data(null)
                    } else {
                        error(GraphQLExceptionWrapper(SourceSystemException(message = it.errorMessage, sourceSystem = "cantus")))
                    }
                }.build()

                imageTag to result
            }

            successes + failures
        } catch (e: SourceSystemException) {
            keys.associateWith { DataFetcherResult.newResult<Image?>().error(GraphQLExceptionWrapper(e)).build() }
        }
    }
}
