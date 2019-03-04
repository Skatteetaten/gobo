package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ImageTagDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : MultipleKeysDataLoader<ImageTag, Instant> {
    override fun getByKeys(user: User, keys: MutableSet<ImageTag>): Map<ImageTag, Instant> {

        val imageRepos = keys.map { it.imageRepository.toImageRepo()}
        val responses = imageRegistryServiceBlocking.findTagsByName(
            imageReposDto = imageRepos,
            imageTags = keys.toList().map { it.name },
            token = user.token
        )

        return keys.associate {imageTag ->
            val imageRepoDto = imageTag.imageRepository.toImageRepo()
            val filteredResponses = responses
                .filter {it.imageTag == imageTag.name && imageRepoDto == it.imageRepoDto}
                .map { it.created ?: Instant.EPOCH }

            imageTag to filteredResponses.first()
        }


    }
}