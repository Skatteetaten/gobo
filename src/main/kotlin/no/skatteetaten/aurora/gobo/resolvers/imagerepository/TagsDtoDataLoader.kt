package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class TagsDtoDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : KeyDataLoader<ImageRepoDto, TagsDto> {
    override suspend fun getByKey(key: ImageRepoDto, context: GoboGraphQLContext): TagsDto {
        return imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(
                imageRepoDto = key,
                token = context.token ?: throw AccessDeniedException("Anonymous user can not get image tags")
        )

    }
}
