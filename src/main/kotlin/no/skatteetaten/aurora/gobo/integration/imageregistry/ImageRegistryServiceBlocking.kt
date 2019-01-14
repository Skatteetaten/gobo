package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.function.Function
import java.util.function.Predicate

data class ImageMetadata(
    val CREATED: String?,
    val DOCKER_CONTENT_DIGEST: String
    /*val AURORA_VERSION: String?,
    val APP_VERSION: String?,
    val JOLOKIA_VERSION: String?,
    val JAVA_VERSION_MINOR: String?,
    val JAVA_VERSION_MAJOR: String?,
    val JAVA_VERSION_BUILD: String?,
    val NODE_VERSION: String?,
    val DOCKER_VERSION: String?*/
) {
    val CREATED_AT: Instant =
        try {
            Instant.parse(CREATED)
        } catch (e: Exception) {
            Instant.EPOCH
        }
}

@Service
class ImageRegistryServiceBlocking(
    private val registryMetadataResolver: RegistryMetadataResolver,
    @TargetService(ServiceTypes.CANTUS) val webClient: WebClient,
    val tokenProvider: TokenProvider
) {

    fun resolveTagToSha(key: ImageTag): String {
        val imageMetadata = getImageMetaData(key.imageRepository.toImageRepo(), key.name)


        return imageMetadata?.DOCKER_CONTENT_DIGEST
            ?: throw SourceSystemException("Could not find digest sha for $key")
    }

    fun findTagByName(imageRepoDto: ImageRepoDto, tagName: String): ImageTagDto {
        return getImageMetaData(imageRepoDto, tagName)?.let {
            ImageTagDto(name = tagName, created = it.CREATED_AT)
        } ?: throw SourceSystemException("No metadata for tag=$tagName in repo=${imageRepoDto.repository}")
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto): List<String> {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        val tagsOrderedByCreatedDate: List<String>? = getTags(imageRepoDto, registryMetadata.authenticationMethod)

        // The current image registry returns the tag names in the order they were created. There does not, however,
        // seem to be a way to affect what property the tags are ordered by or the direction they are ordered in, so
        // there is a chance this order is an "undocumented feature" of the api. We will rely on this feature for the
        // time being, though, as it allows for some queries to be significantly quicker than fetching the individual
        // created dates for each tag, and then sort.
        return tagsOrderedByCreatedDate?.reversed() ?: emptyList()
    }

    private fun getImageMetaData(imageRepoDto: ImageRepoDto, tag: String): ImageMetadata? {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        return try {
            getManifestInformation(imageRepoDto, tag, registryMetadata.authenticationMethod)
        } catch (e: Exception) {
            throw SourceSystemException("Unable to get manifest for image: $tag", e)
        }
    }

    private fun getManifestInformation(
        imageRepoDto: ImageRepoDto,
        imageTag: String,
        authenticationMethod: AuthenticationMethod
    ): ImageMetadata? =
        getFromCantus(
            "/${imageRepoDto.imageName}/$imageTag/manifest",
            authenticationMethod
        )

    private fun getTags(
        imageRepoDto: ImageRepoDto,
        authenticationMethod: AuthenticationMethod
    ): List<String>? =
        getFromCantus("/${imageRepoDto.imageName}/tags", authenticationMethod)

    private final inline fun <reified T : Any> getFromCantus(
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