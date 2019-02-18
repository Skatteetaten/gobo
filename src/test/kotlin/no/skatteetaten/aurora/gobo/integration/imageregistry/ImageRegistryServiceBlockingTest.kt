package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.message
import assertk.assertions.support.expected
import assertk.catch
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import no.skatteetaten.aurora.gobo.integration.setJsonFileAsBody
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.HttpStatus
import java.time.Instant

@MockWebServerTestTag
class ImageRegistryServiceBlockingTest {

    private val imageRepoName = "no_skatteetaten_aurora_demo/whoami"
    private val tagName = "2"

    private val server = MockWebServer()
    private val url = server.url("/")
    private val imageRepo = ImageRepository.fromRepoString("/$imageRepoName").toImageRepo()

    private val defaultRegistryMetadataResolver = mockk<DefaultRegistryMetadataResolver>()
    private val tokenProvider = mockk<TokenProvider>()
    private val imageRegistry = ImageRegistryServiceBlocking(
        defaultRegistryMetadataResolver, WebClient.create(url.toString()), tokenProvider, ImageRegistryUrlBuilder()
    )

    @BeforeEach
    fun setUp() {
        clearMocks(defaultRegistryMetadataResolver, tokenProvider)

        every {
            defaultRegistryMetadataResolver.getMetadataForRegistry(any())
        } returns RegistryMetadata("${url.host()}:${url.port()}", "http", NONE, false)
    }

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

            val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo)
            assertThat(tags).containsAllTags(expectedTags)
        }

        assertThat(request.getRequestPath()).isEqualTo("/$imageRepoName/tags")

        assertThat(request.headers[HttpHeaders.AUTHORIZATION]).isNull()
    }

    @Test
    fun `fetch all tags with authorization header`() {
        every { tokenProvider.token } returns "token"
        every {
            defaultRegistryMetadataResolver.getMetadataForRegistry(any())
        } returns RegistryMetadata("${url.host()}:${url.port()}", "http", KUBERNETES_TOKEN, false)

        val response = MockResponse().setJsonFileAsBody("cantusTags.json")

        val request = server.execute(response) {
            val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo)
            assertThat(tags.tags).isNotNull()
            assertThat(tags.tags).isNotEmpty()
        }

        assertThat(request.getRequestPath()).isEqualTo("/$imageRepoName/tags")

        assertThat(request.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer token")
    }

    @Test
    fun `verify dockerContentDigest can be found`() {
        val response = MockResponse().setJsonFileAsBody("cantusManifest.json")

        val request = server.execute(response) {
            val imageTag = ImageTag(ImageRepository.fromRepoString(imageRepo.repository), tagName)
            val dockerContentDigest = imageRegistry.resolveTagToSha(imageTag)
            assertThat(dockerContentDigest).isEqualTo("sha256:9d044d853c40b42ba52c576e1d71e5cee7dc4d1b328650e0780cd983cb474ed0")
        }

        assertThat(request.getRequestPath()).isEqualTo("/$imageRepoName/$tagName/manifest")
    }

    @Test
    fun `verify tag can be found by name`() {
        val response = MockResponse().setJsonFileAsBody("cantusManifest.json")

        val request = server.execute(response) {
            val tag = imageRegistry.findTagByName(imageRepo, tagName)
            assertThat(tag.created).isEqualTo(Instant.parse("2018-11-05T14:01:22.654389192Z"))
        }

        assertThat(request.getRequestPath()).isEqualTo("/$imageRepoName/$tagName/manifest")
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 418, 500, 501])
    fun `get tags given error from Cantus throw exception`(statusCode: Int) {
        server.execute(statusCode, HttpStatus.valueOf(statusCode)) {
            assertThat {
                imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo)
            }.thrownError {
                given {
                    assertThat(it::class).isEqualTo(SourceSystemException::class)
                    assertThat(it.message).endsWith("status=$statusCode message=${HttpStatus.valueOf(statusCode).reasonPhrase}")
                }
            }

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
        if(it.isNullOrEmpty()) expected("Exception message was null or empty")
        if(it.endsWith(message)) return

        expected("Exception message does not end with the specified message " +
            "\nexpected=$message" +
            "\nactual=$it")
    }
}
