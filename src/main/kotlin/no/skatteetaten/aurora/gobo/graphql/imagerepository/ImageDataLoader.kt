package no.skatteetaten.aurora.gobo.graphql.imagerepository

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class ImageDataLoader(val imageRegistryService: ImageRegistryService) :
    KeyDataLoader<ImageTag, Image?> {
    override suspend fun getByKey(key: ImageTag, ctx: GoboGraphQLContext): Image? {
        val imageReposAndTags = ImageRepoAndTags.fromImageTags(setOf(key))
        // TODO can it contain multiple tags?
        val response = imageRegistryService.findTagsByName(imageReposAndTags, ctx.token()).items.firstOrNull()
        return response?.let { Image(it.timeline.buildEnded, it.requestUrl) }
    }
}
