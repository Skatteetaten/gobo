package no.skatteetaten.aurora.gobo.integration.herkimer

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

class HerkimerServiceReactiveTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc"
    }
    private val webClient = ApplicationConfig(500, "", sharedSecretReader)
        .webClientHerkimer(server.url("/").toString(), WebClient.builder())
    private val herkimerService = HerkimerServiceReactive(webClient, jacksonObjectMapper())

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
            AuroraResponse<ResourceHerkimer, ErrorResponse>(items = listOf(ResourceHerkimer("1")))

        val claimHerkimerResponse =
            AuroraResponse<JsonNode, ErrorResponse>(success = false)

        server.executeBlocking(resourceHerkimerResponse, claimHerkimerResponse) {
            val result = herkimerService.registerResourceAndClaim(defaultRegisterResourceAndClaimCommand)

            assertThat(result.success).isFalse()
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
