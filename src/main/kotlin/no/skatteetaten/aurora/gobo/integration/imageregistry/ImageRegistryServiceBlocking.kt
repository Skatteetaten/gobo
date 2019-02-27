package no.skatteetaten.aurora.gobo.integration.imageregistry

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val logger = KotlinLogging.logger {}

@Service
class ImageRegistryServiceBlocking(
    @TargetService(ServiceTypes.CANTUS) val webClient: WebClient,
    val tokenProvider: TokenProvider,
    private val urlBuilder: ImageRegistryUrlBuilder
) {

    fun resolveTagToSha(imageRepoDto: ImageRepoDto, imageTag: String) =
        findTagByName(imageRepoDto, imageTag).dockerDigest

    fun findTagByName(
        imageRepoDto: ImageRepoDto,
        imageTag: String
    ) = getAuroraResponseImageTagResource(imageRepoDto, imageTag)

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto) =
        TagsDto.toDto(
            execute{
                logger.debug("Retrieving type=TagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}")
                it.get().uri(
                    urlBuilder.createTagsUrl(imageRepoDto),
                    imageRepoDto.mappedTemplateVars
                )
            }
        )


    private fun getAuroraResponseImageTagResource(
        imageRepoDto: ImageRepoDto,
        imageTag: String
    ): ImageTagDto {

        val auroraImageTagResource: AuroraResponse<ImageTagResource> =
            execute {
                logger.debug("Retrieving type=ImageTagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}/$imageTag")
                it.get().uri(
                    urlBuilder.createImageTagUrl(imageRepoDto),
                    imageRepoDto.mappedTemplateVars.plus("tag" to imageTag)
                )
            }

        return ImageTagDto.toDto(auroraImageTagResource, imageTag)
    }

    private final inline fun <reified T : Any> execute(
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): T = fn(webClient)
        .headers {
            it.set("Authorization", "Bearer ${tokenProvider.token}")
        }
        .retrieve()
        .bodyToMono<T>()
        .blockNonNullAndHandleError(sourceSystem = "cantus")
}