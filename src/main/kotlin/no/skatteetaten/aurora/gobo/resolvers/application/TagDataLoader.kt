package no.skatteetaten.aurora.gobo.resolvers.application

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.utils.logLine
import no.skatteetaten.aurora.utils.time
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Component
class TagDataLoader(val imageRegistryService: ImageRegistryService) : KeysDataLoader<ImageRepo, List<String>> {

    private val logger: Logger = LoggerFactory.getLogger(TagDataLoader::class.java)

    override fun getByKeys(keys: List<ImageRepo>): List<List<String>> {

        logger.info("Loading tags for ${keys.size} image repos")

        val sw = StopWatch()
        val imageTags = sw.time("Fetch all tags for all image repos") {
            runBlocking {
                keys.map {
                    async {
                        try {
                            val allTagsFor = imageRegistryService.findAllTagsInRepo(it)
                            allTagsFor.map { it.name }
                        } catch (e: Exception) {
                            emptyList<String>()
                        }
                    }
                }.map { it.await() }
            }
        }

        logger.info("Loaded tags for ${keys.size} image repos. ${sw.logLine}.")

        return imageTags
    }
}