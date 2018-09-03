package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.exceptions.ResolverException
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRegistryService
import no.skatteetaten.aurora.utils.logLine
import no.skatteetaten.aurora.utils.time
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.time.Instant

private class Error(val tag: ImageTag, val e: Exception)
private typealias MaybeInstant = Pair<Instant?, Error?>

@Component
class TagDataLoader(val imageRegistryService: ImageRegistryService) : KeysDataLoader<ImageTag, Instant?> {

    private val logger: Logger = LoggerFactory.getLogger(TagDataLoader::class.java)

    val context = newFixedThreadPoolContext(6, "tag-loader")

    override fun getByKeys(keys: List<ImageTag>): List<Instant?> {

        logger.info("Loading ${keys.size} tags (${keys.toSet().size} unique)")

        val sw = StopWatch()
        val imageTags: List<Pair<Instant?, Error?>> = sw.time("Fetch ${keys.size} tags") {
            runBlocking(context) {
                keys.map { imageTag ->
                    async(context) {
                        val imageRepo = imageTag.imageRepository.toImageRepo()
                        try {
                            val tagByName = imageRegistryService.findTagByName(imageRepo, imageTag.name)
                            val created = tagByName.created
                            MaybeInstant(created, null)
                        } catch (e: Exception) {
                            MaybeInstant(null, Error(imageTag, e))
                        }
                    }
                }.map { it.await() }
            }
        }

        logger.info(sw.logLine)

        val errors = imageTags.map { it.second }.filterNotNull()
        errors.takeIf { it.isNotEmpty() }?.let {
            val exampleError = it.first()
            val tagName = exampleError.tag.name
            val repo = exampleError.tag.imageRepository.repository
            throw ResolverException(
                    "An error occurred loading ${it.size} tags. Example: tag=$tagName, repo=$repo", exampleError.e
            )
        }

        return imageTags.map { it.first }
    }
}