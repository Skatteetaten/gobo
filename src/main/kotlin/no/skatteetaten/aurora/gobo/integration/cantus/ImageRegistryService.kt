package no.skatteetaten.aurora.gobo.integration.cantus

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirst
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepoDto
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.integration.onStatusNotOk
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

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

private val logger = KotlinLogging.logger { }

@Service
class ImageRegistryService(
    @TargetService(ServiceTypes.CANTUS) val webClient: WebClient,
    private val objectMapper: ObjectMapper
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

    suspend fun findImageTagDto(imageRepoDto: ImageRepoDto, imageTag: String, token: String): ImageTagDto {
        val requestBody = BodyInserters.fromValue(
            TagUrlsWrapper(listOf("${imageRepoDto.repository}/$imageTag"))
        )
        val auroraImageTagResource: AuroraResponse<ImageTagResource> = webClient
            .post()
            .uri("/manifest")
            .body(requestBody)
            .execute(token)

        return ImageTagDto.toDto(auroraImageTagResource, imageTag, imageRepoDto)
    }

    suspend fun findTagsByName(
        imageReposAndTags: List<ImageRepoAndTags>,
        token: String
    ): AuroraResponse<ImageTagResource> {
        val tagUrls = imageReposAndTags.getAllTagUrls()
        val requestBody = BodyInserters.fromValue(tagUrls)
        return webClient
            .post()
            .uri("/manifest")
            .body(requestBody)
            .execute(token)
    }

    suspend fun findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoDto: ImageRepoDto, token: String): TagsDto {
        val uri = "/tags?repoUrl={repoUrl}"

        val uriVariables =
            mutableMapOf("repoUrl" to "${imageRepoDto.registry}/${imageRepoDto.mappedTemplateVars["namespace"]}/${imageRepoDto.mappedTemplateVars["imageTag"]}")

        val uriWithFilter = imageRepoDto.filter?.let {
            uriVariables["filter"] = it
            "$uri&filter={filter}"
        } ?: uri

        return webClient.get().uri(uriWithFilter, uriVariables).execute<TagResource>(token).let { TagsDto.toDto(it) }
    }

    private suspend inline fun <reified T : Any> WebClient.RequestHeadersSpec<*>.execute(token: String): AuroraResponse<T> =
        this.headers {
            it.set(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.retrieve()
            .onStatusNotOk { status, body ->
                CantusIntegrationException(
                    message = "Request failed when getting image data",
                    integrationResponse = body,
                    status = status
                )
            }
            .bodyToMono<AuroraResponse<Any>>().map { response ->
                response.run {
                    if (failureCount > 0) {
                        failure.partition {
                            it.errorMessage.contains(
                                "Only v2 manifest is supported",
                                ignoreCase = true
                            )
                        }.let { groupedFailures ->
                            groupedFailures.first.takeIf { it.isNotEmpty() }?.let {
                                logger.debug { "Cantus unsupported manifest version: $it" }
                            }

                            groupedFailures.second.takeIf { it.isNotEmpty() }?.let {
                                logger.warn("Failures from cantus response: $it")
                            }
                        }
                    }

                    AuroraResponse(
                        items = items.map { item ->
                            runCatching {
                                objectMapper.convertValue(item, T::class.java)
                            }.onFailure {
                                logger.error(it) { "Unable to parse response items from cantus: $item" }
                            }.getOrThrow()
                        },
                        failure = failure,
                        success = success,
                        message = message,
                        failureCount = failureCount,
                        successCount = successCount,
                        count = count
                    )
                }
            }.awaitFirst()
}
