package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ImageTagDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : KeyDataLoader<ImageTag, Instant> {
    override fun getByKey(user: User, key: ImageTag): Try<Instant> {
        return Try.tryCall {
            val imageRepo = key.imageRepository.toImageRepo()
            imageRegistryServiceBlocking.findTagByName(imageRepo, key.name).created
        }
    }
}