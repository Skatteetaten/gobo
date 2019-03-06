package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.message
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
import java.time.Instant

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
    fun `verify fetches all tags for specified repo`() {
        val tagsListResponse = MockResponse().setJsonFileAsBody("cantusTags.json")
        val request = server.execute(tagsListResponse) {
            val listOfTags = listOf(
                "SNAPSHOT--dev-20170912.120730-1-b1.4.1-wingnut-8.141.1",
                "dev-SNAPSHOT",
                "latest",
                "1",
                "1.0",
                "1.0.0"
            )

            val expectedTags = TagsDto(
                listOfTags.map {
                    Tag(it, ImageTagType.typeOf(it))
                }
            )

            val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo, token)
            assertThat(tags).containsAllTags(expectedTags)
        }

        assertThat(request.getRequestPath()).isEqualTo("/tags?repoUrl=${imageRepo.repository}")

        assertThat(request.headers[HttpHeaders.AUTHORIZATION]).isNotNull()
    }

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
    fun `verify dockerContentDigest can be found`() {
        val response = MockResponse().setJsonFileAsBody("cantusManifest.json")

        val request = server.execute(response) {
            val dockerContentDigest = imageRegistry.resolveTagToSha(imageRepo, tagName, token)
            assertThat(dockerContentDigest).isEqualTo("sha256:9d044d853c40b42ba52c576e1d71e5cee7dc4d1b328650e0780cd983cb474ed0")
        }

        assertThat(request.getRequestPath()).isEqualTo("/manifest?tagUrl=${imageRepo.repository}/$tagName")
    }

    @Test
    fun `verify tag can be found by name`() {
        val response = MockResponse().setJsonFileAsBody("cantusManifest.json")

        val request = server.execute(response) {
            val tag = imageRegistry.findTagByName(imageRepo, tagName, token)
            assertThat(tag.created).isEqualTo(Instant.parse("2018-11-05T14:01:22.654389192Z"))
        }

        assertThat(request.getRequestPath()).isEqualTo("/manifest?tagUrl=${imageRepo.repository}/$tagName")
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
                .message().endsWith("status=$statusCode message=${HttpStatus.valueOf(statusCode).reasonPhrase}")
        }
    }

    private fun RecordedRequest.getRequestPath() = this.path.replace("%2F", "/")
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

private fun Assert<String?>.endsWith(message: String) {
    given {
        if (it.isNullOrEmpty()) expected("Exception message was null or empty")
        if (it.endsWith(message)) return

        expected(
            "Exception message does not end with the specified message " +
                "\nexpected=$message" +
                "\nactual=$it"
        )
    }
}
