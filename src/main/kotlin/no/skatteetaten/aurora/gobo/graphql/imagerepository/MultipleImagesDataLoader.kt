package no.skatteetaten.aurora.gobo.graphql.imagerepository

import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GraphQLExceptionWrapper
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.integration.cantus.CantusIntegrationException
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import org.springframework.stereotype.Component

@Component
class MultipleImagesDataLoader(private val imageRegistryService: ImageRegistryService) :
    GoboDataLoader<ImageTag, ImageWithType?>() {
    override suspend fun getByKeys(
        keys: Set<ImageTag>,
        ctx: GoboGraphQLContext
    ): Map<ImageTag, ImageWithType?> {
        val imageReposAndTags = ImageRepoAndTags.fromImageTags(keys)
        val auroraResponse = imageRegistryService.findTagsByName(
            imageReposAndTags = imageReposAndTags,
            token = ctx.token()
        )

        val successes = auroraResponse.items.associate { imageTagResource ->
            val imageTag = ImageTag.fromTagString(imageTagResource.requestUrl, "/")
            val image = Image(imageTagResource.timeline.buildEnded, imageTagResource.requestUrl)
            imageTag to newDataFetcherResult(image)
        }

        val failures = auroraResponse.failure.associate {
            val imageTag = ImageTag.fromTagString(it.url, "/")

            val result = DataFetcherResult.newResult<Image?>().apply {
                if (it.errorMessage.contains("application/vnd.docker.distribution.manifest.v1")) {
                    data(null)
                } else {
                    error(GraphQLExceptionWrapper(CantusIntegrationException(message = it.errorMessage)))
                }
            }.build()

            imageTag to result
        }

        return (successes + failures).mapValues {
            when {
                it.value.hasErrors() -> null
                else -> ImageWithType(it.key.name, it.value.data)
            }
        }
    }
}
