package no.skatteetaten.aurora.gobo.integration.gavel

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.CnameInfoBuilder
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class CnameServiceReactiveTest {

    private val server = MockWebServer()
    private val service = CnameServiceReactive(WebClient.create(server.url))

    @Test
    fun `Get cname info`() {
        val requests = server.executeBlocking(listOf(CnameInfoBuilder().build())) {
            val cnameInfo = service.getCnameInfo()
            assertThat(cnameInfo).hasSize(1)
            assertThat(cnameInfo.first().appName).isEqualTo("demo")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get cname info with affiliation`() {
        val requests = server.executeBlocking(
            listOf(
                CnameInfoBuilder("aup").build(),
                CnameInfoBuilder("test").build()
            )
        ) {
            val cnameInfo = service.getCnameInfo("aup")
            assertThat(cnameInfo).hasSize(1)
            assertThat(cnameInfo.first().namespace).isEqualTo("aup")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get cname info with affiliation not existing`() {
        val requests = server.executeBlocking(
            listOf(
                CnameInfoBuilder("aup").build(),
                CnameInfoBuilder("test").build()
            )
        ) {
            val cnameInfo = service.getCnameInfo("aurora")
            assertThat(cnameInfo).hasSize(0)
        }

        assertThat(requests).hasSize(1)
    }
}
