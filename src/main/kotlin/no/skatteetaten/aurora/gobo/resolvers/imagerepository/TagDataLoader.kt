package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.utils.logLine
import no.skatteetaten.aurora.utils.time
import org.dataloader.Try
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.time.Instant

@Component
class TagDataLoader(val imageRegistryService: ImageRegistryService) : KeysDataLoader<ImageTag, Try<Instant>> {

    private val logger: Logger = LoggerFactory.getLogger(TagDataLoader::class.java)

    val context = newFixedThreadPoolContext(6, "tag-loader")

    override fun getByKeys(keys: List<ImageTag>): List<Try<Instant>> {

        logger.debug("Loading ${keys.size} tags (${keys.toSet().size} unique)")

        val sw = StopWatch()
        val imageTags: List<Try<Instant>> = sw.time("Fetch ${keys.size} tags") {
            runBlocking(context) {
                keys.map { imageTag ->
                    async(context) {
                        Try.tryCall {
                            val imageRepo = imageTag.imageRepository.toImageRepo()
                            imageRegistryService.findTagByName(imageRepo, imageTag.name).created
                        }
                    }
                }.map { it.await() }
            }
        }

        logger.debug(sw.logLine)
        return imageTags
    }
}