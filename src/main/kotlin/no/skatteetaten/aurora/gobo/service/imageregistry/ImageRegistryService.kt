package no.skatteetaten.aurora.gobo.service.imageregistry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Instant
import kotlin.reflect.KClass

class ImageRegistryServiceErrorException(message: String, cause: Throwable) : RuntimeException(message, cause)

data class ImageRepo(val registry: String, val namespace: String, val name: String)

enum class ImageTagType {
    LATEST,
    SNAPSHOT,
    MAJOR,
    MINOR,
    BUGFIX,
    AURORA_VERSION,
    AURORA_SNAPSHOT_VERSION;

    companion object {
        fun typeOf(tag: String): ImageTagType {
            return if (tag.toLowerCase().equals("latest")) ImageTagType.LATEST
            else if (tag.toLowerCase().endsWith("-snapshot")) ImageTagType.SNAPSHOT
            else if (tag.toLowerCase().startsWith("snapshot-")) ImageTagType.AURORA_SNAPSHOT_VERSION
            else if (tag.matches(Regex("^\\d+$"))) ImageTagType.MAJOR
            else if (tag.matches(Regex("^\\d+.\\d+$"))) ImageTagType.MINOR
            else if (tag.matches(Regex("^\\d+.\\d+.\\d+$"))) ImageTagType.BUGFIX
            else ImageTagType.AURORA_VERSION
        }
    }
}

data class ImageTag(
    val name: String,
    var created: Instant? = null
) {
    val type: ImageTagType
        get() = ImageTagType.typeOf(name)
}

@Service
class ImageRegistryService(
    private val restTemplate: RestTemplate,
    private val urlBuilder: ImageRegistryUrlBuilder,
    private val registryMetadataResolver: RegistryMetadataResolver,
    private val tokenProvider: TokenProvider
) {

    private val objectMapper = jacksonObjectMapper()

    fun findTagByName(imageRepo: ImageRepo, tagName: String): ImageTag {
        val metadata = getImageMetaData(imageRepo, tagName)
        return ImageTag(name = tagName, created = metadata?.createdDate)
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo: ImageRepo): List<String> {

        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepo.registry)

        val tagListUrl = urlBuilder.createTagListUrl(registryMetadata.apiSchema, imageRepo)

        val tagList = getFromRegistry(registryMetadata, tagListUrl, TagList::class)
        val tagsOrderedByCreatedDate = tagList?.tags ?: emptyList()

        // The current image registry returns the tag names in the order they were created. There does not, however,
        // seem to be a way to affect what property the tags are ordered by or the direction they are ordered in, so
        // there is a chance this order is an "undocumented feature" of the api. We will rely on this feature for the
        // time being, though, as it allows for some queries to be significantly quicker than fetching the individual
        // created dates for each tag, and then sort.
        return tagsOrderedByCreatedDate.reversed()
    }

    private fun getImageMetaData(imageRepo: ImageRepo, tag: String): ImageMetadata? {

        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepo.registry)

        return try {
            val manifestsUrl = urlBuilder.createManifestsUrl(registryMetadata.apiSchema, imageRepo, tag)
            getFromRegistry(registryMetadata, manifestsUrl, String::class)?.let { parseMainfest(it) }
        } catch (e: Exception) {
            throw ImageRegistryServiceErrorException("Unable to get manifest for image: $tag", e)
        }
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

    private fun <T : Any> getFromRegistry(registryMetadata: RegistryMetadata, apiUrl: String, kClass: KClass<T>): T? {

        val headers = HttpHeaders().apply {
            if (registryMetadata.authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                set("Authorization", "Bearer ${tokenProvider.token}")
            }
        }
        val request = RequestEntity<T>(headers, HttpMethod.GET, URI(apiUrl))
        return try {
            restTemplate.exchange(request, kClass.java).body
        } catch (e: HttpClientErrorException) {
            if (e.statusCode != HttpStatus.NOT_FOUND) {
                throw e
            }
            null
        }
    }
}