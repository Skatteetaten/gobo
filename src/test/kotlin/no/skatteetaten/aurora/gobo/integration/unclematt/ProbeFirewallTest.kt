package no.skatteetaten.aurora.gobo.integration.unclematt

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

class ProbeFireWallTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val probeService = ProbeService(WebClient.create(url.toString()))

    @Test
    fun `happy day`() {
        server.executeBlocking(probeResponse) {
            val probeResultList = probeService.probeFirewall("server.test.no", 9999)
            assertThat(probeResultList.size).isEqualTo(2)
        }
    }

    @Test
    fun `throws correct exception when backend returns 404`() {
        server.executeBlocking(404 to Response<String>(message = "something went wrong", items = emptyList())) {
            assertThat {
                probeService.probeFirewall("server.test.no", 9999)
            }.isNotNull().isFailure().all {
                isInstanceOf(WebClientResponseException::class).messageContains("404 Not Found")
            }
        }
    }
}

private const val probeResponse =
    """[
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
