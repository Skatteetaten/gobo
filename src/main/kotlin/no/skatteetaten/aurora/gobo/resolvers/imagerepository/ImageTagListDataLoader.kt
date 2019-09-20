package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoDto
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class ImageTagListDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : KeyDataLoader<ImageRepoDto, TagsDto> {
    override fun getByKey(user: User, key: ImageRepoDto): Try<TagsDto> {
        return Try.tryCall {
            imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(
                imageRepoDto = key,
                token = user.token
            )
        }
    }
}
