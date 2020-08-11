package no.skatteetaten.aurora.gobo.integration.unclematt

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.assertions.prop
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
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
            assertThat(probeResultList.size).isEqualTo(2)
        }
    }

    @Test
    fun `throws correct exception when backend returns 404`() {

        server.execute(404 to Response<String>(message = "something went wrong", items = emptyList())) {
            assertThat {
                probeService.probeFirewall("server.test.no", 9999)
            }.isNotNull().isFailure().all {
                isInstanceOf(SourceSystemException::class).prop(SourceSystemException::errorMessage).contains("404")
                message().isNotNull().isEqualTo("something went wrong")
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
