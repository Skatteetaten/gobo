package no.skatteetaten.aurora.gobo.integration.cantus

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirst
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepoDto
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageTag
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.q3c.rest.hal.HalResource

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
        val filterQueryParam = imageRepoDto.filter?.let {
            "&filter=$it"
        } ?: ""
        val resource = webClient.get().uri(
            "/tags?repoUrl=${imageRepoDto.registry}/{namespace}/{imageTag}$filterQueryParam",
            imageRepoDto.mappedTemplateVars
        ).execute<TagResource>(token)
        return TagsDto.toDto(resource)
    }

    private suspend inline fun <reified T : HalResource> WebClient.RequestHeadersSpec<*>.execute(token: String) =
        this.headers {
            it.set(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.retrieve().bodyToMono<AuroraResponse<T>>().map { response ->
            response.copy(items = response.items.map { objectMapper.convertValue(it, T::class.java) })
        }.awaitFirst()
}
