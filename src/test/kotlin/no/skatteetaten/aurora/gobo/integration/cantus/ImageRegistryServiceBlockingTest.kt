package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.catch
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.AuroraResponseBuilder
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class ImageRegistryServiceBlockingTest {

    private val imageRepoName = "no_skatteetaten_aurora_demo/whoami"

    private val server = MockWebServer()
    private val url = server.url("/")
    private val imageRepo = ImageRepository.fromRepoString("docker.com/$imageRepoName").toImageRepo()

    private val token: String = "token"
    private val imageRegistry = ImageRegistryServiceBlocking(
        WebClient.create(url.toString())
    )

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
}
