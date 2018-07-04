package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.Instant
import kotlin.reflect.KClass

data class ImageRepo(
    val registryUrl: String,
    val namespace: String,
    val name: String
) {
    override fun toString(): String {
        return listOf(registryUrl, namespace, name).joinToString("/")
    }

    companion object {
        /**
         * @param absoluteImageRepoPath Example docker-registry.aurora.sits.no:5000/no_skatteetaten_aurora/dbh
         */
        fun fromRepoString(absoluteImageRepoPath: String): ImageRepo {
            val (registryUrl, namespace, name) = absoluteImageRepoPath.split("/")
            return ImageRepo(registryUrl, namespace, name)
        }
    }
}

interface ImageRegistryUrlBuilder {
    fun createManifestsUrl(imageRepo: ImageRepo, tag: String): String

    fun createTagListUrl(imageRepo: ImageRepo): String

    fun createApiUrl(imageRepo: ImageRepo): String
}

@Component
class DefaultImageRegistryUrlBuilder : ImageRegistryUrlBuilder {

    override fun createManifestsUrl(imageRepo: ImageRepo, tag: String) = "${createApiUrl(imageRepo)}/manifests/$tag"

    override fun createTagListUrl(imageRepo: ImageRepo) = "${createApiUrl(imageRepo)}/tags/list"

    override fun createApiUrl(imageRepo: ImageRepo) =
        "https://${imageRepo.registryUrl}/v2/${imageRepo.namespace}/${imageRepo.name}"
}

@Component
@Primary
@ConditionalOnProperty("docker-registry.url")
class OverrideRegistryImageRegistryUrlBuilder(
    @Value("\${docker-registry.url}") val registryUrl: String
) : DefaultImageRegistryUrlBuilder() {

    override fun createApiUrl(imageRepo: ImageRepo): String {
        return "$registryUrl/v2/${imageRepo.namespace}/${imageRepo.name}"
    }
}

@Service
class ImageRegistryService(private val restTemplate: RestTemplate, private val urlBuilder: ImageRegistryUrlBuilder) {

    private val logger: Logger = LoggerFactory.getLogger(ImageRegistryService::class.java)

    private val objectMapper = jacksonObjectMapper()

    fun findAllTagsInRepo(imageRepo: ImageRepo): List<ImageTag> {

        val sw = StopWatch()
        val tagList = sw.time("Find tag names") { findTagNamesForImageRepo(imageRepo) }
        val tags = sw.time("Create tags") { findTagsByNames(imageRepo, tagList) }

        logger.info("Fetched ${tags.size} tags with metadata for image repo $imageRepo. ${sw.logLine}.")

        return tags
    }

    private fun findTagNamesForImageRepo(imageRepo: ImageRepo): List<String> {

        val tagListUrl = urlBuilder.createTagListUrl(imageRepo)
        val responseEntity = restTemplate.getForObjectNullOnNotFound(tagListUrl, TagList::class)
        return responseEntity?.tags ?: emptyList()
    }

    private fun findTagsByNames(imageRepo: ImageRepo, tagNames: List<String>): List<ImageTag> {
        return runBlocking {
            tagNames.map {
                async {
                    val metadata = getImageMetaData(imageRepo, it)
                    ImageTag(name = it, created = metadata?.createdDate)
                }
            }.map { it.await() }
        }
    }

    private fun getImageMetaData(imageRepo: ImageRepo, tag: String): ImageMetadata? =
        try {
            val manifestsUrl = urlBuilder.createManifestsUrl(imageRepo, tag)
            restTemplate.getForObjectNullOnNotFound(manifestsUrl, String::class)?.let { parseMainfest(it) }
        } catch (e: Exception) {
            throw ImageRegistryServiceErrorException("Unable to get manifest for image: $tag", e)
        }

    private fun parseMainfest(manifestString: String): ImageMetadata {

        val manifest = objectMapper.readTree(manifestString)
        val manifestHistory = manifest.get("history").get(0).get("v1Compatibility")
        val manifestFirstHistory = objectMapper.readTree(manifestHistory.asText())
        val createdString = manifestFirstHistory.get("created").asText()
        return ImageMetadata(Instant.parse(createdString))
    }

    private data class TagList(
        var name: String,
        var tags: List<String>
    )

    private data class ImageMetadata(val createdDate: Instant)
}

class ImageRegistryServiceErrorException(message: String, cause: Throwable) : RuntimeException(message, cause)

data class ImageTag(
    val name: String,
    var created: Instant? = null
)

private fun <T : Any> RestTemplate.getForObjectNullOnNotFound(url: String, kClass: KClass<T>): T? {
    return try {
        this.getForObject(url, kClass.java)
    } catch (e: HttpClientErrorException) {
        if (e.statusCode != HttpStatus.NOT_FOUND) {
            throw e
        }
        null
    }
}

private fun <T> StopWatch.time(taskName: String, function: () -> T): T {
    this.start(taskName)
    val res = function()
    this.stop()
    return res
}

private val StopWatch.logLine: String
    get() = this.taskInfo.joinToString { "${it.taskName}: ${it.timeMillis}ms" }
