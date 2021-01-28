package no.skatteetaten.aurora.gobo.integration.herkimer

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.graphql.credentials.PostgresHerkimerDatabaseInstance
import no.skatteetaten.aurora.gobo.integration.containsAuroraToken
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class HerkimerServiceTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc"
    }
    private val webClient = ApplicationConfig(500, 500, 500, "", sharedSecretReader)
        .webClientHerkimer(server.url("/").toString(), WebClient.builder())
    private val herkimerService = HerkimerServiceReactive(webClient, jacksonObjectMapper())

    @Test
    fun `Fails to register resource`() {
        val resourceHerkimerResponse =
            AuroraResponse<ResourceHerkimer, ErrorResponse>(errors = listOf(ErrorResponse("Nothing works")))

        server.executeBlocking(500 to resourceHerkimerResponse ) {
            val result = herkimerService.registerResourceAndClaim(
                RegisterResourceAndClaimCommand(
                    ownerId = "12345",
                    credentials = PostgresHerkimerDatabaseInstance("host", 5432, "instance", "admin", "pass", "aurora"),
                    resourceName = "resourceName",
                    claimName = "claimName",
                    resourceKind = ResourceKind.PostgresDatabaseInstance
                )
            )

            assertThat(result.success).isFalse()
        }
    }
}
