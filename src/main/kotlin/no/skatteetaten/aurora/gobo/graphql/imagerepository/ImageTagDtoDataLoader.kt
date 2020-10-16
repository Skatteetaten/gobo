package no.skatteetaten.aurora.gobo.graphql.imagerepository

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagDto
import no.skatteetaten.aurora.gobo.graphql.AccessDeniedException
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class ImageTagDtoDataLoader(private val imageRegistryServiceBlocking: ImageRegistryServiceBlocking) :
    KeyDataLoader<ImageTag, ImageTagDto> {

    override suspend fun getByKey(key: ImageTag, context: GoboGraphQLContext): ImageTagDto {

        val imageRepoDto = key.imageRepository.toImageRepo()
        return imageRegistryServiceBlocking.findImageTagDto(
            imageRepoDto,
            key.name,
            token = context.token ?: throw AccessDeniedException("Anonymous user can not get image tags")
        )
    }
}
