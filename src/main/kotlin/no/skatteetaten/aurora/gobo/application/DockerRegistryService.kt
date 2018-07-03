package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.lang.String.format
import java.time.Instant
import java.util.stream.StreamSupport
import kotlin.reflect.KClass
import kotlin.streams.toList

class DockerRegistryService(
        val restTemplate: RestTemplate,
        val dockerRegistryUrl: String,
        val objectMapper: ObjectMapper = ObjectMapper()
) {
    fun findAllTagsFor(dockerImageName: String): List<DockerTag> {

        val tagListUrl = getTagListUrl(dockerImageName)
        val responseEntity = restTemplate.getForEntity(tagListUrl, DockerTagList::class.java)
        val dockerTagList = responseEntity.body!!
        return dockerTagList.tags.map {
            val dockerTag = DockerTag(name = it)
            addDockerMetaData(dockerImageName, dockerTag)
            dockerTag
        }
    }

    private fun addDockerMetaData(imageGroupAndName: String, dockerTag: DockerTag) {

        val manifestsUrl = getManifestsUrl(imageGroupAndName, dockerTag.name)

        val manifestString = try {
            restTemplate.getForObjectNullOnNotFound(manifestsUrl, String::class)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode != HttpStatus.NOT_FOUND) {
                throw DockerServiceErrorException("Unable to get Docker manifest for image: ${dockerTag.name}", e)
            }
            null
        } catch (e: RestClientException) {
            throw DockerServiceErrorException("Unable to get Docker manifest for image: ${dockerTag.name}", e)
        }

        manifestString?.let {
            try {
                addDockerMetaDataFromManifest(dockerTag, it)
            } catch (e: IOException) {
                throw DockerServiceErrorException("Could not parse Docker manifest", e)
            }
        }
    }

    @Throws(IOException::class)
    private fun addDockerMetaDataFromManifest(dockerTag: DockerTag, manifestString: String) {

        val manifest = objectMapper.readTree(manifestString)
        val manifestHistory = manifest.get("history").get(0).get("v1Compatibility")
        val manifestFirstHistory = objectMapper.readTree(manifestHistory.asText())
        val containerEnvs = manifestFirstHistory.get("container_config").get("Env")
        val lines = StreamSupport.stream(containerEnvs.spliterator(), false)
                .map(JsonNode::asText).toList()
        val envs = lines.map {
            val (key, value) = it.split("=");
            key to value
        }

        dockerTag.created = Instant.parse(manifestFirstHistory.get("created").asText()).toEpochMilli()
        dockerTag.envVars = envs
    }

    private fun getManifestsUrl(imageGroupAndName: String, tag: String) = "${getApiUrl(imageGroupAndName)}/manifests/$tag"

    private fun getTagListUrl(imageGroupAndName: String) = "${getApiUrl(imageGroupAndName)}/tags/list"

    private fun getApiUrl(imageGroupAndName: String) = "$dockerRegistryUrl/v2/$imageGroupAndName"

}

class DockerServiceErrorException(message: String, cause: Throwable) : RuntimeException(message, cause)


data class DockerTagList(
        var name: String,
        var tags: List<String>
)

data class DockerSpec(
        var name: String,
        var tags: List<DockerTag> = emptyList()
)

data class DockerTag(
        val name: String,
        var created: Long? = null,
        var envVars: List<Pair<String, String>>? = null
) {

    val auroraVersion: String
        get() = findMetaValue("AURORA_VERSION")

    val appVersion: String
        get() = findMetaValue("APP_VERSION")

    val runtimeType: String
        get() = findMetaValue("RUNTIME_TYPE", "java")

    val runtimeVersion: String
        get() = if ("java".equals(runtimeType, ignoreCase = true)) javaVersion else ""

    val baseImageVersion: String
        get() = findMetaValue("BASE_IMAGE_VERSION")

    private val javaVersion: String
        get() {
            val major = findMetaValue("JAVA_VERSION_MAJOR")
            val minor = findMetaValue("JAVA_VERSION_MINOR")

            return if (major.isBlank()) "" else format("%su%s", major, minor)
        }

    private fun findMetaValue(key: String, defaultValue: String = ""): String {

        return this.envVars
                ?.filter { keyValue -> key.equals(keyValue.first, ignoreCase = true) }
                ?.map { it.second }
                ?.firstOrNull() ?: defaultValue
    }
}

fun <T : Any> RestTemplate.getForObjectNullOnNotFound(url: String, kClass: KClass<T>): T? {
    return try {
        this.getForObject(url, kClass.java)
    } catch (e: HttpClientErrorException) {
        if (e.statusCode != HttpStatus.NOT_FOUND) {
            throw e
        }
        null
    }
}