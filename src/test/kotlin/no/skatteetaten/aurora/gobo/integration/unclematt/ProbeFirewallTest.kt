package no.skatteetaten.aurora.gobo.integration.unclematt

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.catch
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class ProbeFireWallTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val probeService = ProbeServiceBlocking(ProbeService(WebClient.create(url.toString())))

    @Test
    fun `happy day`() {
        server.execute(probeResponse) {
            val probeResultList = probeService.probeFirewall("server.test.no", 9999)
            assert(probeResultList.size).isEqualTo(2)
        }
    }

    @Test
    fun `throws correct exception when backend returns 404`() {
        val exception = catch {
            server.execute(404, "") {
                probeService.probeFirewall("server.test.no", 9999)
            }
        }
        assert(exception).isNotNull { t ->
            t.isInstanceOf(SourceSystemException::class)
            t.message().isNotNull {
                it.contains("404")
            }
        }
    }
}

private val probeResponse = """[
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
