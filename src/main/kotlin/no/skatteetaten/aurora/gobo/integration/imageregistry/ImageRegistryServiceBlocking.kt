package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo

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

    fun resolveTagToSha(key: ImageTag): String {
        val imageTagDto: ImageTagDto = getAuroraResponseImageTagResource(key.imageRepository.toImageRepo(key.name))

        return imageTagDto.dockerDigest
    }

    fun findTagByName(imageRepoDto: ImageRepoDto): ImageTagDto {
        return getAuroraResponseImageTagResource(imageRepoDto)
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto): TagsDto {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)


        return TagsDto.toDto(execute(registryMetadata.authenticationMethod) {
            it.get().uri(
                urlBuilder.createTagsUrl(imageRepoDto, registryMetadata),
                imageRepoDto.mappedTemplateVars
            )
        }
        )
    }

    private fun getAuroraResponseImageTagResource(imageRepoDto: ImageRepoDto): ImageTagDto {
        val registryMetadata = registryMetadataResolver.getMetadataForRegistry(imageRepoDto.registry)

        val auroraImageTagResource: AuroraResponse<ImageTagResource> = execute(registryMetadata.authenticationMethod) {
            it.get().uri(
                urlBuilder.createImageTagUrl(imageRepoDto, registryMetadata),
                imageRepoDto.mappedTemplateVars
            )

            }
        return ImageTagDto.toDto(auroraImageTagResource)
    }



    private final inline fun <reified T : Any> execute(
        authenticationMethod: AuthenticationMethod,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): T = fn(webClient)
        .headers {
            //TODO: This logic is in Cantus. How to do this properly? Gobo should always send a token to Cantus
            if (authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                it.set("Authorization", "Bearer ${tokenProvider.token}")
            }
        }
        .retrieve()
        .bodyToMono<T>()
        .blockNonNullAndHandleError(sourceSystem = "cantus")
}