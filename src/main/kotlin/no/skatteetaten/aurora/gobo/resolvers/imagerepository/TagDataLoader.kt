package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRegistryService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.utils.logLine
import no.skatteetaten.aurora.utils.time
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.time.Instant

@Component
class TagDataLoader(val imageRegistryService: ImageRegistryService) : KeysDataLoader<ImageTag, Instant> {

    private val logger: Logger = LoggerFactory.getLogger(TagDataLoader::class.java)

    val context = newFixedThreadPoolContext(6, "tag-loader")

    override fun getByKeys(keys: List<ImageTag>): List<Instant> {

        logger.info("Loading ${keys.size} tags (${keys.toSet().size} unique)")

        val sw = StopWatch()
        val imageTags = sw.time("Fetch ${keys.size} tags") {
            runBlocking(context) {
                keys.map {
                    async(context) {
                        try {
                            imageRegistryService.findTagByName(it.imageRepo, it.name).created ?: Instant.EPOCH
                        } catch (e: Exception) {
                            Instant.EPOCH
                        }
                    }
                }.map { it.await() }
            }
        }

        logger.info(sw.logLine)

        return imageTags
    }
}