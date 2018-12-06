package no.skatteetaten.aurora.gobo.integration.imageregistry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.function.Function
import java.util.function.Predicate

data class TagList(var name: String, var tags: List<String>)
data class ImageMetadata(val createdDate: Instant)

@Service
class ImageRegistryServiceBlocking(
    private val urlBuilder: ImageRegistryUrlBuilder,
    private val registryMetadataResolver: RegistryMetadataResolver,
    @TargetService(ServiceTypes.DOCKER) val webClient: WebClient,
    val tokenProvider: TokenProvider
) {

    private val objectMapper = jacksonObjectMapper()

    fun resolveTagToSha(key: ImageTag): String {

        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(key.imageRepository.registryUrl)

        val manifestsUrl =
            urlBuilder.createManifestsUrl(registryMetadata.apiSchema, key.imageRepository.toImageRepo(), key.name)

        val response: Mono<ClientResponse> = webClient
            .get()
            .uri(manifestsUrl)
            .headers {
                it.accept = listOf(MediaType.valueOf("application/vnd.docker.distribution.manifest.v2+json"))
                if (registryMetadata.authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                    it.set("Authorization", "Bearer ${tokenProvider.token}")
                }
            }
            .exchange()

        val headers: Mono<String?> = response.map { it.headers().header("Docker-Content-Digest").firstOrNull() }
        return headers.blockAndHandleError() ?: throw SourceSystemException("Could not find digest sha for $key")
    }

    fun findTagByName(imageRepoDto: ImageRepoDto, tagName: String): ImageTagDto {
        return getImageMetaData(imageRepoDto, tagName)?.let {
            ImageTagDto(name = tagName, created = it.createdDate)
        } ?: throw SourceSystemException("No metadata for tag=$tagName in repo=${imageRepoDto.repository}")
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto): List<String> {

        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        val tagListUrl = urlBuilder.createTagListUrl(registryMetadata.apiSchema, imageRepoDto)

        val tagList: TagList? = getTags(tagListUrl, registryMetadata.authenticationMethod)
        val tagsOrderedByCreatedDate = tagList?.tags ?: emptyList()

        // The current image registry returns the tag names in the order they were created. There does not, however,
        // seem to be a way to affect what property the tags are ordered by or the direction they are ordered in, so
        // there is a chance this order is an "undocumented feature" of the api. We will rely on this feature for the
        // time being, though, as it allows for some queries to be significantly quicker than fetching the individual
        // created dates for each tag, and then sort.
        return tagsOrderedByCreatedDate.reversed()
    }

    private fun getImageMetaData(imageRepoDto: ImageRepoDto, tag: String): ImageMetadata? {

        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        return try {
            val manifestsUrl = urlBuilder.createManifestsUrl(registryMetadata.apiSchema, imageRepoDto, tag)
            getManifest(manifestsUrl, registryMetadata.authenticationMethod)
                ?.let { parseManifest(it) }
        } catch (e: Exception) {
            throw SourceSystemException("Unable to get manifest for image: $tag", e)
        }
    }

    private fun parseManifest(manifestString: String): ImageMetadata {

        val manifest = objectMapper.readTree(manifestString)
        val manifestHistory = manifest.get("history").get(0).get("v1Compatibility")
        val manifestFirstHistory = objectMapper.readTree(manifestHistory.asText())
        val createdString = manifestFirstHistory.get("created").asText()
        return ImageMetadata(Instant.parse(createdString))
    }

    private fun getTags(apiUrl: String, authenticationMethod: AuthenticationMethod): TagList? {
        return getFromRegistry(apiUrl, authenticationMethod)
    }

    private fun getManifest(apiUrl: String, authenticationMethod: AuthenticationMethod): String? {
        return getFromRegistry(apiUrl, authenticationMethod)
    }

    private final inline fun <reified T : Any> getFromRegistry(
        apiUrl: String,
        authenticationMethod: AuthenticationMethod
    ): T? = webClient
        .get()
        .uri(apiUrl)
        .headers {
            if (authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                it.set("Authorization", "Bearer ${tokenProvider.token}")
            }
        }
        .retrieve()
        .onStatus(Predicate.isEqual<HttpStatus>(HttpStatus.NOT_FOUND), Function { Mono.empty() })
        .bodyToMono<T>()
        .block()
}