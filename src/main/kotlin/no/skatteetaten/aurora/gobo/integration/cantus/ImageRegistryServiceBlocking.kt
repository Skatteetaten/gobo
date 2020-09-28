package no.skatteetaten.aurora.gobo.integration.cantus

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.handleError
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepoDto
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import uk.q3c.rest.hal.HalResource

private val logger = KotlinLogging.logger {}

data class TagUrlsWrapper(val tagUrls: List<String>)

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
    TagUrlsWrapper(this.flatMap { it.getTagUrls() })

@Service
class ImageRegistryServiceBlocking(
    @TargetService(ServiceTypes.CANTUS) val webClient: WebClient
) {

    suspend fun resolveTagToSha(imageRepoDto: ImageRepoDto, imageTag: String, token: String): String? {
        val requestBody = BodyInserters.fromValue(
            TagUrlsWrapper(listOf("${imageRepoDto.repository}/$imageTag"))
        )
        val auroraImageTagResource: AuroraResponse<ImageTagResource> = webClient
            .post()
            .uri("/manifest")
            .body(requestBody)
            .execute(token)

        return ImageTagDto.toDto(auroraImageTagResource, imageTag, imageRepoDto).dockerDigest
    }

    fun findImageTagDto(imageRepoDto: ImageRepoDto, imageTag: String, token: String): ImageTagDto {
        val requestBody = BodyInserters.fromValue(
            TagUrlsWrapper(listOf("${imageRepoDto.repository}/$imageTag"))
        )
        val auroraImageTagResource: AuroraResponse<ImageTagResource> =
            execute<AuroraResponse<ImageTagResource>>(token) {
                logger.debug("Retrieving type=ImageTagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}/$imageTag")
                it.post().uri("/manifest").body(requestBody)
            }.block()!!
        return ImageTagDto.toDto(auroraImageTagResource, imageTag, imageRepoDto)
    }

    fun findTagsByName(
        imageReposAndTags: List<ImageRepoAndTags>,
        token: String
    ): AuroraResponse<ImageTagResource> {
        val tagUrls = imageReposAndTags.getAllTagUrls()
        val requestBody = BodyInserters.fromValue(tagUrls)

        return execute<AuroraResponse<ImageTagResource>>(token) {
            it.post().uri("/manifest").body(requestBody)
        }.block()!!
    }

    fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto, token: String) =
        TagsDto.toDto(
            execute<AuroraResponse<TagResource>>(token) { client ->
                logger.debug("Retrieving type=TagResource from  url=${imageRepoDto.registry} image=${imageRepoDto.imageName}")

                val filterQueryParam = imageRepoDto.filter?.let {
                    "&filter=$it"
                } ?: ""
                client.get().uri(
                    "/tags?repoUrl=${imageRepoDto.registry}/{namespace}/{imageTag}$filterQueryParam",
                    imageRepoDto.mappedTemplateVars
                )
            }.blockAndHandleCantusFailure()
        )

    private inline fun <reified T : HalResource> AuroraResponse<T>.responseOrNull(): T? =
        this.items.ifEmpty { null }?.first()

    private inline fun <reified T : HalResource> AuroraResponse<T>.responses(): List<T> = this.items

    private suspend inline fun <reified T : HalResource> WebClient.RequestHeadersSpec<*>.execute(token: String) =
        this.headers {
            it.set(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.retrieve().awaitBody<AuroraResponse<T>>()

    private inline fun <reified T : Any> execute(
        token: String,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): Mono<T> = fn(webClient)
        .headers {
            it.set("Authorization", "Bearer $token")
        }
        .retrieve()
        .bodyToMono<T>()
        .handleGenericError()

    private fun <T> Mono<T>.handleGenericError(): Mono<T> =
        this.handleError("cantus")
            .switchIfEmpty(SourceSystemException("Empty response", sourceSystem = "cantus").toMono())

    private fun <T> Mono<T>.blockAndHandleCantusFailure(): T =
        this.flatMap {
            if (it is AuroraResponse<*> && it.failureCount > 0) {
                Mono.error(SourceSystemException(message = it.message, sourceSystem = "cantus"))
            } else {
                Mono.just(it)
            }
        }.block()!!
}
