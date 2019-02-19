package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class ImageRegistryServiceBlocking(
    private val registryMetadataResolver: RegistryMetadataResolver,
    @TargetService(ServiceTypes.CANTUS) val webClient: WebClient,
    val tokenProvider: TokenProvider,
    private val urlBuilder: ImageRegistryUrlBuilder
) {

    fun resolveTagToSha(imageRepoDto: ImageRepoDto): String {
        val imageTagDto: ImageTagDto = getAuroraResponseImageTagResource(imageRepoDto)

        return imageTagDto.dockerDigest
    }

    fun findTagByName(imageRepoDto: ImageRepoDto): ImageTagDto {
        return getAuroraResponseImageTagResource(imageRepoDto)
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto): TagsDto {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        logger.debug("Retrieving type=TagResource from  url=${registryMetadata.registry} image=${imageRepoDto.imageName}")
        return TagsDto.toDto(
            execute(registryMetadata.authenticationMethod) {
                it.get().uri(
                    urlBuilder.createTagsUrl(imageRepoDto, registryMetadata),
                    imageRepoDto.mappedTemplateVars
                )
            }
        )
    }

    private fun getAuroraResponseImageTagResource(imageRepoDto: ImageRepoDto): ImageTagDto {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        logger.debug("Retrieving type=ImageTagResource from  url=${registryMetadata.registry} image=${imageRepoDto.imageName}")
        val auroraImageTagResource: AuroraResponse<ImageTagResource> =
            execute(registryMetadata.authenticationMethod) {
                it.get().uri(
                    urlBuilder.createImageTagUrl(imageRepoDto, registryMetadata),
                    imageRepoDto.mappedTemplateVars
                )
            }

        return ImageTagDto.toDto(auroraImageTagResource, imageRepoDto.tag)
    }

    private final inline fun <reified T : Any> execute(
        authenticationMethod: AuthenticationMethod,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): T = fn(webClient)
        .headers {
            if (authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                it.set("Authorization", "Bearer ${tokenProvider.token}")
            }
        }
        .retrieve()
        .bodyToMono<T>()
        .blockNonNullAndHandleError(sourceSystem = "cantus")
}