package no.skatteetaten.aurora.gobo.graphql.imagerepository

import graphql.GraphQLContext
import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import org.springframework.stereotype.Component

@Component
class VersionsDataLoader(private val imageRegistryService: ImageRegistryService) : GoboDataLoader<ImageRepository, DataFetcherResult<List<ImageTag>>>() {

    override suspend fun getByKeys(keys: Set<ImageRepository>, ctx: GraphQLContext): Map<ImageRepository, DataFetcherResult<List<ImageTag>>> =
        keys.associateWith { key ->
            runCatching {
                when {
                    key.isFullyQualified() ->
                        imageRegistryService
                            .findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto = key.toImageRepo(), token = ctx.token)
                            .tags
                            .toImageTags(key)
                            .toList()
                    else -> emptyList()
                }.let(::newDataFetcherResult)
            }.recoverCatching(::newDataFetcherResult).getOrThrow()
        }

    private fun List<Tag>.toImageTags(imageRepository: ImageRepository) = this
        .map { ImageTag(imageRepository = imageRepository, name = it.name) }
}
