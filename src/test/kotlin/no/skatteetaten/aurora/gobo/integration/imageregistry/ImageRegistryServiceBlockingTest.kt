package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.message
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
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

class ImageRegistryServiceBlockingTest {

    private val imageRepoName = "no_skatteetaten_aurora_demo/whoami"
    private val tagName = "2"

    private val server = MockWebServer()
    private val url = server.url("/")
    private val imageRepo = ImageRepository.fromRepoString("${url.host()}:${url.port()}/$imageRepoName").toImageRepo()

    private val defaultRegistryMetadataResolver = mockk<DefaultRegistryMetadataResolver>()
    private val tokenProvider = mockk<TokenProvider>()
    private val imageRegistry = ImageRegistryServiceBlocking(
        defaultRegistryMetadataResolver, WebClient.create(url.toString()), tokenProvider
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
            assert(tags).containsAll(
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

<<<<<<< refs/remotes/origin/feature/AOS-3089-ta-i-bruk-cantus-i-gobo
        assert(request.getRequestPath()).isEqualTo("/$imageRepoName/tags")
=======
        assert(request.path).isEqualTo("/$imageRepoName/tags")
>>>>>>> HEAD~5
        assert(request.headers[HttpHeaders.AUTHORIZATION]).isNull()
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
            assert(tags).isNotNull()
        }

<<<<<<< refs/remotes/origin/feature/AOS-3089-ta-i-bruk-cantus-i-gobo
        assert(request.getRequestPath()).isEqualTo("/$imageRepoName/tags")
=======
        assert(request.path).isEqualTo("/$imageRepoName/tags")
>>>>>>> HEAD~5
        assert(request.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer token")
    }

    @Test
    fun `verify tag can be found by name`() {
        val response = MockResponse().setJsonFileAsBody("cantusManifest.json")

        val request = server.execute(response) {
            val tag = imageRegistry.findTagByName(imageRepo, tagName)
            assert(tag.created).isEqualTo(Instant.parse("2018-11-05T14:01:22.654389192Z"))
            assert(tag.name).isEqualTo(tagName)
            assert(tag.type).isEqualTo(ImageTagType.MAJOR)
        }

<<<<<<< refs/remotes/origin/feature/AOS-3089-ta-i-bruk-cantus-i-gobo
        assert(request.getRequestPath()).isEqualTo("/$imageRepoName/$tagName/manifest")
=======
        assert(request.path).isEqualTo("/$imageRepoName/$tagName/manifest")
>>>>>>> HEAD~5
    }

    @Test
    fun `Throw exception when bad request is returned from registry`() {
        server.execute(404, "Not found") {
            assert {
                imageRegistry.findTagByName(imageRepo, tagName)
            }.thrownError {
                message().isEqualTo("Error in response, status:404 message:Not Found")
            }
        }
    }
<<<<<<< refs/remotes/origin/feature/AOS-3089-ta-i-bruk-cantus-i-gobo

    private fun RecordedRequest.getRequestPath() = this.path.replace("%2F", "/")
=======
>>>>>>> HEAD~5
}