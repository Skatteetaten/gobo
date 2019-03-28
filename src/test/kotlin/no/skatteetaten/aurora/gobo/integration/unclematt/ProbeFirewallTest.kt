package no.skatteetaten.aurora.gobo.integration.unclematt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.catch
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class ProbeFireWallTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val probeService = ProbeServiceBlocking(ProbeService(WebClient.create(url.toString())))

    @Test
    fun `happy day`() {
        server.execute(probeResponse) {
            val probeResultList = probeService.probeFirewall("server.test.no", 9999)
            assertThat(probeResultList.size).isEqualTo(2)
        }
    }

    @Test
    fun `throws correct exception when backend returns 404`() {
        val exception = catch {
            server.execute(404 to "") {
                probeService.probeFirewall("server.test.no", 9999)
            }
        }
        assertThat(exception).isNotNull()
            .isInstanceOf(SourceSystemException::class)
            .message().isNotNull().contains("404")
    }
}

private const val probeResponse = """[
    {
        "result": {
        "status": "OPEN",
        "message": "Brannmur er åpen",
        "dnsname": "server.test.no",
        "resolvedIp": "192.168.1.1",
        "port": "9999"
      },
      "podIp": "192.168.10.1",
      "hostIp": "192.168.100.1"
    },
    {
      "result": {
        "status": "CLOSED",
        "message": "Brannmur ikke åpen",
        "dnsname": "server.test.no",
        "resolvedIp": "192.168.1.1",
        "port": "9999"
      },
      "podIp": "192.168.10.2",
      "hostIp": "192.168.100.2"
    }
]"""
