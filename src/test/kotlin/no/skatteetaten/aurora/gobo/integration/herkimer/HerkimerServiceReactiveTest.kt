package no.skatteetaten.aurora.gobo.integration.herkimer

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import assertk.assertThat
import assertk.assertions.isFalse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.graphql.credentials.PostgresHerkimerDatabaseInstance
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import assertk.assertions.messageContains
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.HerkimerResourceBuilder
import no.skatteetaten.aurora.gobo.security.PsatSecretReader
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.TestObjectMapperConfigurer

class HerkimerServiceReactiveTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc"
    }
    private val psatSecretReader = mockk<PsatSecretReader> {
        every { secret } returns mapOf("mock" to "abc")
    }
    private val webClient = ApplicationConfig(500, 500, 300000, "", sharedSecretReader, psatSecretReader)
        .webClientHerkimer(server.url("/").toString(), WebClient.builder())
    private val herkimerService = HerkimerServiceReactive(webClient, jacksonObjectMapper())

    @BeforeEach
    fun setUp() {
        TestObjectMapperConfigurer.objectMapper = testObjectMapper()
    }

    @AfterEach
    fun tearDown() {
        TestObjectMapperConfigurer.reset()
    }

    @Test
    fun `Fails to register resource with 500`() {
        val resourceHerkimerResponse =
            AuroraResponse<ResourceHerkimer, ErrorResponse>(errors = listOf(ErrorResponse("Nothing works")))

        server.executeBlocking(500 to resourceHerkimerResponse) {
            val result = herkimerService.registerResourceAndClaim(defaultRegisterResourceAndClaimCommand)

            assertThat(result.success).isFalse()
        }
    }

    @Test
    fun `Fails to register resource with success false`() {
        val resourceHerkimerResponse =
            AuroraResponse<ResourceHerkimer, ErrorResponse>(
                success = false,
                errors = listOf(ErrorResponse("Nothing works"))
            )

        server.executeBlocking(resourceHerkimerResponse) {
            val result = herkimerService.registerResourceAndClaim(defaultRegisterResourceAndClaimCommand)

            assertThat(result.success).isFalse()
        }
    }

    @Test
    fun `Fails to claim resource`() {
        val resourceHerkimerResponse =
            AuroraResponse<ResourceHerkimer, ErrorResponse>(items = listOf(HerkimerResourceBuilder("1").build()))

        val claimHerkimerResponse =
            AuroraResponse<JsonNode, ErrorResponse>(success = false)

        server.executeBlocking(resourceHerkimerResponse, claimHerkimerResponse) {
            val result = herkimerService.registerResourceAndClaim(defaultRegisterResourceAndClaimCommand)

            assertThat(result.success).isFalse()
        }
    }

    @Test
    fun `Should be able to get resource`() {
        val resource =
            AuroraResponse<ResourceHerkimer, ErrorResponse>(items = listOf(HerkimerResourceBuilder("1").build()))

        server.executeBlocking(resource) {
            val result = runBlocking {
                herkimerService.getResourceWithClaim("somehting", ResourceKind.StorageGridTenant)
            }

            assertThat(result).isNotNull()
            assertThat(result?.id).isNotNull().isNotEmpty()
        }
    }

    @Test
    fun `Should return null when no resources`() {
        val resource = AuroraResponse<ResourceHerkimer, ErrorResponse>(
            items = emptyList()
        )

        server.executeBlocking(resource) {
            assertThat {
                herkimerService.getResourceWithClaim("somehting", ResourceKind.StorageGridTenant)
            }.isSuccess().isNull()
        }
    }

    @Test
    fun `Should handle failure from herkimer`() {
        val response = AuroraResponse<ResourceHerkimer, ErrorResponse>()
        server.executeBlocking(500 to response) {
            assertThat {
                herkimerService.getResourceWithClaim("somehting", ResourceKind.StorageGridTenant)
            }.isFailure().messageContains("Error when retrieving resource from herkimer")
        }
    }

    @Test
    fun `Should handle multiple resources as an illegalstate`() {
        val resource = AuroraResponse<ResourceHerkimer, ErrorResponse>(
            items = listOf(
                HerkimerResourceBuilder("1").build(),
                HerkimerResourceBuilder("1").build()
            )
        )

        server.executeBlocking(resource) {
            assertThat {
                herkimerService.getResourceWithClaim("somehting", ResourceKind.StorageGridTenant)
            }.isFailure().given {
                assertThat(it).isInstanceOf(HerkimerIntegrationException::class)
                    .messageContains("Expected only one resource")
            }
        }
    }

    private val defaultRegisterResourceAndClaimCommand = RegisterResourceAndClaimCommand(
        ownerId = "12345",
        credentials = PostgresHerkimerDatabaseInstance("instance", "host", 5432, "admin", "pass", "aurora"),
        resourceName = "resourceName",
        claimName = "claimName",
        resourceKind = ResourceKind.PostgresDatabaseInstance
    )
}
