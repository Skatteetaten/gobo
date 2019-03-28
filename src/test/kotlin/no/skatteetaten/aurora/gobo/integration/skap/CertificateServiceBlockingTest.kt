package no.skatteetaten.aurora.gobo.integration.skap

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.CertificateBuilder
import no.skatteetaten.aurora.gobo.createObjectMapper
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceReactive
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class CertificateServiceBlockingTest {

    private val server = MockWebServer()

    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "test-token"
    }
    private val certificateService =
        CertificateServiceBlocking(CertificateService(sharedSecretReader, WebClient.create(server.url("/").toString())))

    @Test
    fun `Get certificates`() {
        val certificate1 = CertificateBuilder().build()
        val certificate2 = CertificateBuilder(id = "2", dn = ".atomhopper").build()

        val request = server.execute(listOf(certificate1, certificate2), objectMapper = createObjectMapper()) {
            val certificates = certificateService.getCertificates()
            assertThat(certificates.size).isEqualTo(2)
        }.first()

        assertThat(request).containsAuroraToken()
    }

    private fun Assert<RecordedRequest>.containsAuroraToken() = given { request ->
        request.headers[HttpHeaders.AUTHORIZATION]?.let {
            if (it.startsWith(DatabaseSchemaServiceReactive.HEADER_AURORA_TOKEN)) return
        }
        expected("Authorization header to contain ${DatabaseSchemaServiceReactive.HEADER_AURORA_TOKEN}")
    }
}