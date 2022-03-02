package no.skatteetaten.aurora.gobo.graphql.imagerepository

import graphql.GraphQLContext
import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import org.springframework.stereotype.Component

data class ImageTagsKey(val imageRepository: ImageRepository, val filter: String?)

@Component
class ImageTagsConnectionDataLoader(private val imageRegistryService: ImageRegistryService) : GoboDataLoader<ImageTagsKey, DataFetcherResult<ImageTagsConnection>>() {
    override suspend fun getByKeys(keys: Set<ImageTagsKey>, ctx: GraphQLContext): Map<ImageTagsKey, DataFetcherResult<ImageTagsConnection>> {
        return keys.associateWith { key ->
            runCatching {
                val allEdges = when {
                    key.imageRepository.isFullyQualified() -> {
                        val tagsDto = imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(
                            imageRepoDto = key.imageRepository.toImageRepo(key.filter),
                            token = ctx.token
                        )
                        val tags = tagsDto.tags
                        val imageTags = tags.toImageTags(key.imageRepository)
                        imageTags.map { ImageTagEdge(it) }
                    }
                    else -> emptyList()
                }

                newDataFetcherResult(ImageTagsConnection(allEdges))
            }.recoverCatching {
                newDataFetcherResult(it)
            }.getOrThrow()
        }
    }

    private fun List<Tag>.toImageTags(imageRepository: ImageRepository) = this
        .map { ImageTag(imageRepository = imageRepository, name = it.name) }
}
