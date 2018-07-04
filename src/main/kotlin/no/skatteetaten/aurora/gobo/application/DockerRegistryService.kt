package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.Instant
import kotlin.reflect.KClass

@Service
class DockerRegistryService(
    private val restTemplate: RestTemplate,
    @Value("\${docker-registry.url}") private val dockerRegistryUrl: String
) {
    private val logger: Logger = LoggerFactory.getLogger(DockerRegistryService::class.java)

    private val objectMapper = jacksonObjectMapper()

    fun findAllTagsFor(dockerImageName: String): List<DockerTag> {

        val sw = StopWatch()
        val dockerTagList = sw.time("Find tags names") { findTagNamesForDockerImage(dockerImageName) }
        val dockerTags = sw.time("Create tags") { findDockerTagsByNames(dockerImageName, dockerTagList) }

        logger.info("Fetched ${dockerTags.size} tags with metadata for image $dockerImageName. ${sw.logLine}.")

        return dockerTags
    }

    private fun findTagNamesForDockerImage(dockerImageName: String): List<String> {

        val tagListUrl = getTagListUrl(dockerImageName)
        val responseEntity = restTemplate.getForObjectNullOnNotFound(tagListUrl, DockerTagList::class)
        return responseEntity?.tags ?: emptyList()
    }

    private fun findDockerTagsByNames(dockerImageName: String, tagNames: List<String>): List<DockerTag> {
        return runBlocking {
            tagNames.map {
                async {
                    val metadata = getDockerMetaData(dockerImageName, it)
                    DockerTag(name = it, created = metadata?.createdDate)
                }
            }.map { it.await() }
        }
    }

    private fun getDockerMetaData(imageGroupAndName: String, tag: String): DockerMetadata? =
        try {
            val manifestsUrl = getManifestsUrl(imageGroupAndName, tag)
            restTemplate.getForObjectNullOnNotFound(manifestsUrl, String::class)?.let { parseMainfest(it) }
        } catch (e: Exception) {
            throw DockerServiceErrorException("Unable to get Docker manifest for image: $tag", e)
        }

    private fun parseMainfest(manifestString: String): DockerMetadata {

        val manifest = objectMapper.readTree(manifestString)
        val manifestHistory = manifest.get("history").get(0).get("v1Compatibility")
        val manifestFirstHistory = objectMapper.readTree(manifestHistory.asText())
        val createdString = manifestFirstHistory.get("created").asText()
        return DockerMetadata(Instant.parse(createdString))
    }

    private fun getManifestsUrl(image: String, tag: String) = "${getApiUrl(image)}/manifests/$tag"

    private fun getTagListUrl(image: String) = "${getApiUrl(image)}/tags/list"

    private fun getApiUrl(image: String) = "$dockerRegistryUrl/v2/$image"

    private data class DockerTagList(
        var name: String,
        var tags: List<String>
    )

    private data class DockerMetadata(val createdDate: Instant)
}

class DockerServiceErrorException(message: String, cause: Throwable) : RuntimeException(message, cause)

data class DockerTag(
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
