package no.skatteetaten.aurora.gobo.integration.cantus

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.handleError
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

private val logger = KotlinLogging.logger {}

data class ImageRepoAndTags(val imageRepository: String, val imageTags: List<String>) {
    fun getTagUrls() = imageTags.map { "$imageRepository/$it" }

    companion object {
        fun fromImageTags(imageTags: Set<ImageTag>) =
            imageTags.groupBy { it.imageRepository.repository }.map { entry ->
                val imageTagStrings = entry.value.map { it.name }
                ImageRepoAndTags(entry.key, imageTagStrings)

            }
    }
}

private fun List<ImageRepoAndTags>.getAllTagUrls() =
    this.flatMap { it.getTagUrls() }

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
        imageReposAndTags: List<ImageRepoAndTags>,
        token: String
    ): AuroraResponse<ImageTagResource> {

        val tagPath = UriComponentsBuilder
            .fromPath("/manifest")
            .queryParam("tagUrls", imageReposAndTags.getAllTagUrls())
            .build()
            .toUriString()

        return execute(token) { it.get().uri(tagPath) }
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

    private final inline fun <reified T : Any> execute(
        token: String,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): T = fn(webClient)
        .headers {
            it.set("Authorization", "Bearer $token")
        }
        .retrieve()
        .bodyToMono<T>()
        .blockAndHandleCantusFailure()

    private fun <T : Any> Mono<T>.blockAndHandleCantusFailure(): T =
        this.handleError("cantus")
            .switchIfEmpty(SourceSystemException("Empty response", sourceSystem = "cantus").toMono())
            .map {
                if (it is AuroraResponse<*>) {
                    if (it.failureCount > 0) {
                        throw SourceSystemException(it.failure[0].errorMessage)
                    }
                }
                it
            }.block()!!
}
