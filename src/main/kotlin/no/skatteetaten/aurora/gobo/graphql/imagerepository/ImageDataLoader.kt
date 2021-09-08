package no.skatteetaten.aurora.gobo.graphql.imagerepository

import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import org.springframework.stereotype.Component

@Component
class ImageDataLoader(private val imageRegistryService: ImageRegistryService) : GoboDataLoader<ImageTag, DataFetcherResult<Image?>>() {
    override suspend fun getByKeys(keys: Set<ImageTag>, ctx: GoboGraphQLContext): Map<ImageTag, DataFetcherResult<Image?>> {
        return keys.associateWith { key ->
            val imageReposAndTags = ImageRepoAndTags.fromImageTags(setOf(key))
            // TODO can it contain multiple tags?
            runCatching {
                val response = imageRegistryService.findTagsByName(imageReposAndTags, ctx.token()).items.firstOrNull()
                newDataFetcherResult(data = response?.let { Image(it.timeline.buildEnded, it.requestUrl) })
            }.recoverCatching {
                newDataFetcherResult(it)
            }.getOrThrow()
        }
    }
}
