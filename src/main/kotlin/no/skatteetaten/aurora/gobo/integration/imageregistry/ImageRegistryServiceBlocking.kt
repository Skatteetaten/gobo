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

    fun resolveTagToSha(imageRepoDto: ImageRepoDto, imageTag: String): String {
        val imageTagDto: ImageTagDto = findTagByName(imageRepoDto, imageTag)

        return imageTagDto.dockerDigest
    }

    fun findTagByName(
        imageRepoDto: ImageRepoDto,
        imageTag: String
    ): ImageTagDto {
        return getAuroraResponseImageTagResource(imageRepoDto, imageTag)
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto): TagsDto {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        return TagsDto.toDto(
            execute(
                authenticationMethod = registryMetadata.authenticationMethod
            ) {
                logger.debug("Retrieving type=TagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}")
                it.get().uri(
                    urlBuilder.createTagsUrl(imageRepoDto, registryMetadata),
                    imageRepoDto.mappedTemplateVars
                )
            }
        )
    }

    private fun getAuroraResponseImageTagResource(
        imageRepoDto: ImageRepoDto,
        imageTag: String
    ): ImageTagDto {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        val auroraImageTagResource: AuroraResponse<ImageTagResource> =
            execute(registryMetadata.authenticationMethod) {
                logger.debug("Retrieving type=ImageTagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}/$imageTag")
                it.get().uri(
                    urlBuilder.createImageTagUrl(imageRepoDto, registryMetadata),
                    imageRepoDto.mappedTemplateVars.plus("tag" to imageTag)
                )
            }

        return ImageTagDto.toDto(auroraImageTagResource, imageTag)
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