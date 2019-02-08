package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant

data class ImageMetadata(
    val CREATED: String?,
    val DOCKER_CONTENT_DIGEST: String
) {
    val createdAt: Instant =
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
            ImageTagDto(name = tagName, created = it.createdAt)
        } ?: throw SourceSystemException("No metadata for tag=$tagName in repo=${imageRepoDto.repository}")
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto): ImageTagsDto {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        return ImageTagsDto.toDto(execute(registryMetadata.authenticationMethod) {
            it.get().uri("/no_skatteetaten_aurora_demo/whoami/tags")
        } ?: AuroraResponse())
    }

    private fun getImageMetaData(imageRepoDto: ImageRepoDto, tag: String): ImageMetadata? {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        return execute(registryMetadata.authenticationMethod) {
            it.get().uri("/{imageRepoDto.imageName}/{tag}/manifest", imageRepoDto.imageName, tag)

        }
    }

    private final inline fun <reified T : Any> execute(
        authenticationMethod: AuthenticationMethod,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): T? = fn(webClient)
        .headers {
            //TODO: This logic is in Cantus. How to do this properly? Gobo should always send a token to Cantus
            if (authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                it.set("Authorization", "Bearer ${tokenProvider.token}")
            }
        }
        .retrieve()
        .bodyToMono<T>()
        .blockAndHandleError(sourceSystem = "cantus")
}