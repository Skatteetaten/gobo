package no.skatteetaten.aurora.gobo.graphql.imagerepository

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagDto
import org.springframework.stereotype.Component

@Component
class ImageTagDtoDataLoader(private val imageRegistryService: ImageRegistryService) :
    KeyDataLoader<ImageTag, ImageTagDto> {

    override suspend fun getByKey(key: ImageTag, context: GoboGraphQLContext): ImageTagDto {
        val imageRepoDto = key.imageRepository.toImageRepo()
        return imageRegistryService.findImageTagDto(
            imageRepoDto,
            key.name,
            token = context.token()
        )
    }
}
