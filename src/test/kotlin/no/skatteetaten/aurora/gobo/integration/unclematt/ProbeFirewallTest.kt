package no.skatteetaten.aurora.gobo.integration.unclematt

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.createJsonMockResponse
import org.junit.jupiter.api.Test
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient

class ProbeFireWallTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val probeService = ProbeService(WebClient.create(url.toString()))

    @Test
    fun `happy day`() {
        server.enqueue(createJsonMockResponse(body = probeResponse))

        val probeResultList = probeService.probeFirewall("server.test.no", 9999)

        assert(probeResultList.size).isEqualTo(2)
    }

    @Test
    fun `throws correct exception when backend returns 404`() {
        server.enqueue(createJsonMockResponse(status = 404, body = ""))

        assertThrows<SourceSystemException> {
            probeService.probeFirewall("server.test.no", 9999)
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
