package no.skatteetaten.aurora.gobo.graphql.imagerepository

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class ImageDataLoader(val imageRegistryServiceBlocking: ImageRegistryServiceBlocking) :
    KeyDataLoader<ImageTag, Image?> {
    override suspend fun getByKey(key: ImageTag, ctx: GoboGraphQLContext): Image? {
        val imageReposAndTags = ImageRepoAndTags.fromImageTags(setOf(key))
        // TODO can it contain multiple tags?
        val response = imageRegistryServiceBlocking.findTagsByName(imageReposAndTags, ctx.token!!).items.first()
        return Image(response.timeline.buildEnded, response.requestUrl)
    }
}
