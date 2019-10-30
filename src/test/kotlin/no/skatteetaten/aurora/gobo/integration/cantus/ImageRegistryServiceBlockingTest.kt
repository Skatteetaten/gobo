package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.AuroraResponseBuilder
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient

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
            assertThat {
                imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo, token)
            }.isNotNull().isFailure().isInstanceOf(SourceSystemException::class)
                .messageContains("status=$statusCode message=${HttpStatus.valueOf(statusCode).reasonPhrase}")
        }
    }

    @Test
    fun `getTagsByName given non existing tag for image return AuroraResponse with CantusFailure`() {

        val repository = "docker1.no/no_skatteetaten_aurora_demo/whoami"
        val tag = "10"

        val auroraResponse = AuroraResponseBuilder(status = 404, url = "$repository/$tag").build()

        val imageRepoAndTags =
            listOf(ImageRepoAndTags(repository, listOf(tag)))

        server.execute(auroraResponse) {
            val auroraResponseFailure = imageRegistry.findTagsByName(imageRepoAndTags, token)
            assertThat(auroraResponseFailure.failureCount).isEqualTo(1)
            assertThat(auroraResponseFailure.failure.first().url).isNotEmpty()
            assertThat(auroraResponseFailure.failure.first().errorMessage).endsWith("status=404 message=Not Found")
        }
    }
}
