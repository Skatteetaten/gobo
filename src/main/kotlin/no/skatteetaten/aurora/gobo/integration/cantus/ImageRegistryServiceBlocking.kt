package no.skatteetaten.aurora.gobo.integration.cantus

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
    @TargetService(ServiceTypes.CANTUS) val webClient: WebClient
) {

    fun resolveTagToSha(imageRepoDto: ImageRepoDto, imageTag: String, token: String) =
        getAuroraResponseImageTagResource(imageRepoDto, imageTag, token).dockerDigest

    fun findTagByName(
        imageRepoDto: ImageRepoDto,
        imageTag: String,
        token: String
    ) = getAuroraResponseImageTagResource(imageRepoDto, imageTag, token)

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto, token: String) =
        TagsDto.toDto(
            execute(token) {
                logger.debug("Retrieving type=TagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}")
                it.get().uri(
                    "/tags?repoUrl=${imageRepoDto.registry}/{namespace}/{name}",
                    imageRepoDto.mappedTemplateVars
                )
            }
        )

    private fun getAuroraResponseImageTagResource(
        imageRepoDto: ImageRepoDto,
        imageTag: String,
        token: String
    ): ImageTagDto {

        val auroraImageTagResource: AuroraResponse<ImageTagResource> =
            execute(token) {
                logger.debug("Retrieving type=ImageTagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}/$imageTag")
                it.get().uri(
                    "/manifest?tagUrl=${imageRepoDto.registry}/{namespace}/{name}/{tag}",
                    imageRepoDto.mappedTemplateVars.plus("tag" to imageTag)
                )
            }

        return ImageTagDto.toDto(auroraImageTagResource, imageTag)
    }

    private final inline fun <reified T : Any> execute(
        token: String,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): T = fn(webClient)
        .headers {
            it.set("Authorization", "Bearer $token")
        }
        .retrieve()
        .bodyToMono<T>()
        .blockNonNullAndHandleError(sourceSystem = "cantus")
}