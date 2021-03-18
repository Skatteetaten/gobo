package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.skatteetaten.aurora.gobo.AuroraResponseBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepoDto
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepository
import no.skatteetaten.aurora.mockmvc.extensions.TestObjectMapperConfigurer
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.jsonResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.reactive.function.client.WebClient

class ImageRegistryServiceTest {

    private val imageRepoName = "no_skatteetaten_aurora_demo/whoami"

    private val server = MockWebServer()
    private val url = server.url("/")
    private val imageRepo = ImageRepository.fromRepoString("docker.com/$imageRepoName").toImageRepo()

    private val token: String = "token"
    private val objectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private val imageRegistry = ImageRegistryService(
        WebClient.create(url.toString()),
        objectMapper
    )

    @BeforeEach
    fun setUp() {
        TestObjectMapperConfigurer.objectMapper = objectMapper
    }

    @AfterEach
    fun tearDown() {
        TestObjectMapperConfigurer.objectMapper = jacksonObjectMapper()
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 418, 500, 501])
    fun `get tags given error from Cantus throw exception`(statusCode: Int) {
        val response: AuroraResponse<ImageTagResource> = AuroraResponseBuilder(status = statusCode, url = "").build()
        val mockResponse = jsonResponse(response)
            .setResponseCode(statusCode)

        server.executeBlocking(mockResponse) {
            assertThat {
                imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo, token)
            }.isNotNull().isFailure().isInstanceOf(CantusIntegrationException::class)
                .messageContains("Request failed when getting image data")
        }
    }

    @Test
    fun `getTagsByName given non existing tag for image return AuroraResponse with CantusFailure`() {

        val repository = "docker1.no/no_skatteetaten_aurora_demo/whoami"
        val tag = "10"

        val auroraResponseFailure: AuroraResponse<ImageTagResource> =
            AuroraResponseBuilder(status = 404, url = "$repository/$tag").build()

        val imageRepoAndTags =
            listOf(ImageRepoAndTags(repository, listOf(tag)))

        server.executeBlocking(auroraResponseFailure) {
            val tags = imageRegistry.findTagsByName(imageRepoAndTags, token)
            assertThat(tags.failureCount).isEqualTo(1)
            assertThat(tags.failure.first().url).isNotEmpty()
            assertThat(tags.failure.first().errorMessage).endsWith("status=404 message=Not Found")
        }
    }

    @Test
    fun `findImageTagDto returns partial ImageTagDto result from Cantus`() {
        val partialResult =
            AuroraResponse(
                success = false,
                items = listOf(ImageTagResourceBuilder().build()),
                failure = listOf(CantusFailure("http://localhost", "something went wrong"))
            )

        server.executeBlocking(partialResult) {
            val response =
                imageRegistry.findImageTagDto(ImageRepoDto(null, "namespace", "name", null), "imageTag", token)
            assertThat(response).isNotNull()
            assertThat(response.imageTag).isEqualTo("imageTag")
            assertThat(response.dockerDigest).isNotNull()
        }
    }
}
