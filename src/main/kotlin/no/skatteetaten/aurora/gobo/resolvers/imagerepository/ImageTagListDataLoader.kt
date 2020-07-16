package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.MyGraphQLContext
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoDto
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import org.springframework.stereotype.Component

@Component
class ImageTagListDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : KeyDataLoader<ImageRepoDto, TagsDto> {

    override suspend fun getByKey(key: ImageRepoDto, ctx: MyGraphQLContext): TagsDto {
        return imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(
            imageRepoDto = key,
            token = "user.token" // FIXME user token
        )
    }
}
