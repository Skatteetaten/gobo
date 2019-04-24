package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.CertificateResourceBuilder
import no.skatteetaten.aurora.gobo.createObjectMapper
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.containsAuroraToken
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class CertificateServiceBlockingTest {

    private val server = MockWebServer()

    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "test-token"
    }
    private val certificateService =
        CertificateServiceBlocking(
            CertificateServiceReactive(
                sharedSecretReader,
                WebClient.create(server.url("/").toString())
            )
        )

    @Test
    fun `Get certificates`() {
        val certificate1 = CertificateResourceBuilder().build()
        val certificate2 = CertificateResourceBuilder(id = "2", dn = ".atomhopper").build()

        val request = server.execute(listOf(certificate1, certificate2), objectMapper = createObjectMapper()) {
            val certificates = certificateService.getCertificates()
            assertThat(certificates.size).isEqualTo(2)
        }.first()

        assertThat(request).containsAuroraToken()
    }
}