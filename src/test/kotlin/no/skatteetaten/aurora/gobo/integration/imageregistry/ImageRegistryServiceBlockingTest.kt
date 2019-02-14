package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.message
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import no.skatteetaten.aurora.gobo.integration.setJsonFileAsBody
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

@MockWebServerTestTag
class ImageRegistryServiceBlockingTest {

    private val imageRepoName = "no_skatteetaten_aurora_demo/whoami"
    private val tagName = "2"

    private val server = MockWebServer()
    private val url = server.url("/")
    private val imageRepo = ImageRepository.fromRepoString("/$imageRepoName").toImageRepo(tagName)

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
            val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo)
            assertThat(tags.tags).containsAll(
                "2",
                "foo",
                "jarle_test-SNAPSHOT",
                "latest",
                "martintest",
                "prod",
                "test",
                "testing",
                "trigger_test-SNAPSHOT",
                "yo"
            )
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
            assertThat(tags).isNotNull()
        }

        assertThat(request.getRequestPath()).isEqualTo("/$imageRepoName/tags")

        assertThat(request.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer token")
    }

    @Test
    fun `verify tag can be found by name`() {
        val response = MockResponse().setJsonFileAsBody("cantusManifest.json")

        val request = server.execute(response) {
            val tag = imageRegistry.findTagByName(imageRepo)
            assertThat(tag.created).isEqualTo(Instant.parse("2018-11-05T14:01:22.654389192Z"))
        }

        assertThat(request.getRequestPath()).isEqualTo("/$imageRepoName/$tagName/manifest")
    }

    @Test
    fun `Throw exception when bad request is returned from registry`() {
        server.execute(404, "Not found") {
            assertThat {
                imageRegistry.findTagByName(imageRepo)
            }.thrownError {
                message().isEqualTo("Error in response, status:404 message:Not Found")
            }
        }
    }

    private fun RecordedRequest.getRequestPath() = this.path.replace("%2F", "/")
}