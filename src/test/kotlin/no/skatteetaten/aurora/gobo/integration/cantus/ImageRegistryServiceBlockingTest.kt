package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.Assert
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.support.expected
import assertk.catch
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.AuroraResponseBuilder
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.setJsonFileAsBody
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class ImageRegistryServiceBlockingTest {

    private val imageRepoName = "no_skatteetaten_aurora_demo/whoami"
    private val tagName = "2"

    private val server = MockWebServer()
    private val url = server.url("/")
    private val imageRepo = ImageRepository.fromRepoString("docker.com/$imageRepoName").toImageRepo()

    private val token: String = "token"
    private val imageRegistry = ImageRegistryServiceBlocking(
        WebClient.create(url.toString())
    )

    @Test
    fun `fetch all tags with authorization header`() {

        val response = MockResponse().setJsonFileAsBody("cantusTags.json")

        val request = server.execute(response) {
            val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo, token)
            assertThat(tags.tags).isNotNull()
            assertThat(tags.tags).isNotEmpty()
        }

        assertThat(request.getRequestPath()).isEqualTo("/tags?repoUrl=${imageRepo.repository}")

        assertThat(request.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer token")
    }

    @Test
    fun `getTagsByName given non existing tag for image return AuroraResponse with CantusFailure`() {
        val response = MockResponse().setJsonFileAsBody("cantusManifestFailure.json")
        val repository = "docker1.no/no_skatteetaten_aurora_demo/whoami"
        val tag = "20"
        val imageRepoAndTags =
            listOf(ImageRepoAndTags(repository, listOf(tag)))

        val request = server.execute(response) {
            val auroraResponseFailure = imageRegistry.findTagsByName(imageRepoAndTags, token)
            assertThat(auroraResponseFailure.failureCount).isGreaterThan(0)
            assertThat(auroraResponseFailure.failure.first().url).isEqualTo("$repository/$tag")
            assertThat(auroraResponseFailure.failure.first().errorMessage).endsWith("status=404 message=Not Found")
        }

        assertThat(request.getRequestPath()).isEqualTo("/manifest?tagUrls=$repository/$tag")
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 418, 500, 501])
    fun `get tags given error from Cantus throw exception`(statusCode: Int) {
        val response = AuroraResponseBuilder(status = statusCode, url = "").build()
        val mockResponse = MockResponse()
            .setBody(jacksonObjectMapper().writeValueAsString(response))
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")

        server.execute(mockResponse) {
            val exception = catch { imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo, token) }
            assertThat(exception).isNotNull()
                .isInstanceOf(SourceSystemException::class)

            assertThat(
                exception?.message ?: ""
            ).endsWith("status=$statusCode message=${HttpStatus.valueOf(statusCode).reasonPhrase}")
        }
    }

    private fun RecordedRequest.getRequestPath() =
        this.path
            .replace("%2F", "/")
            .replace("%5D", "")
            .replace("%5B", "")
            .replace("%20", "")

    private fun Assert<TagsDto>.containsAllTags(expectedTags: TagsDto) =
        given { tagsDto ->
            if (
                expectedTags.tags.all { expectedTag ->
                    tagsDto.tags.any { actualTag ->
                        actualTag.name == expectedTag.name
                    }
                }) return

            expected("Some tags were not present")
        }
}
