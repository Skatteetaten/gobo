package no.skatteetaten.aurora.gobo.integration.cantus

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.handleError

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

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

    fun findTagsByName(
        imageReposDto: List<ImageRepoDto>,
        imageTags: List<String>,
        token: String
    ): List<ImageTagDto> {
        val pairedImageTagsAndRepos = imageTags zip imageReposDto


        val uri = "/manifest?${pairedImageTagsAndRepos.getAllTagUrls()}"
        val nothing = ""
        val auroraImageTagResource: AuroraResponse<ImageTagResource> =
            execute(token) {
                it.get().uri (
                    "/manifest?${pairedImageTagsAndRepos.getAllTagUrls()}"

                    )
            }

        return auroraImageTagResource.items.map { imageTagResource ->
            val requestUrlParts = imageTagResource.requsestUrl.split("/")

            val imageRepoDto = ImageRepoDto.fromList(requestUrlParts.dropLast(1))

            ImageTagDto(
                dockerDigest = imageTagResource.dockerDigest,
                imageTag = requestUrlParts.last(),
                imageRepoDto = imageRepoDto,
                created = imageTagResource.timeline.buildEnded

        ) }
    }


    fun List<Pair<String, ImageRepoDto>>.getAllTagUrls() =
        this.joinToString(separator = "&") { pairsOfTagAndRepo ->
            "tagUrls=${pairsOfTagAndRepo.second.repository}/${pairsOfTagAndRepo.first}"
        }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto, token: String) =
        TagsDto.toDto(
            execute(token) {
                logger.debug("Retrieving type=TagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}")
                it.get().uri(
                    "/tags?repoUrl=${imageRepoDto.registry}/{namespace}/{imageTag}",
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
                    "/manifest?tagUrl=${imageRepoDto.registry}/{namespace}/{imageTag}/{tag}",
                    imageRepoDto.mappedTemplateVars.plus("tag" to imageTag)
                )
            }

        return ImageTagDto.toDto(auroraImageTagResource, imageTag, imageRepoDto)
    }

    private final inline fun <reified T: Any> execute(
        token: String,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): T = fn(webClient)
        .headers {
            it.set("Authorization", "Bearer $token")
        }
        .retrieve()
        .bodyToMono<T>()
        .blockAndHandleCantusFailure()

    private fun <T: Any> Mono<T>.blockAndHandleCantusFailure(): T =
        this.handleError("cantus")
            .switchIfEmpty(SourceSystemException("Empty response", sourceSystem = "cantus").toMono())
            .map {
            if(it is AuroraResponse<*>) {
               if (it.failureCount > 0) {
                   throw SourceSystemException(it.failure[0].errorMessage)
               }
            }
            it
        }.block()!!
}
