package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class AuroraConfigServiceTest {

    private val server = MockWebServer()
    private val auroraConfigService =
        AuroraConfigService(BooberWebClient(server.url, WebClient.create(), jacksonObjectMapper()))

    @Test
    fun `Get aurora config`() {
        val response = Response(items = listOf(AuroraConfig("name", "ref", "resolvedRef", emptyList())))
        val requests = server.executeBlocking(response) {
            val auroraConfig = auroraConfigService.getAuroraConfig("token", "auroraConfig", "master")
            assertThat(auroraConfig.name).isEqualTo("name")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get aurora config application files`() {
        val response =
            Response(
                items = listOf(
                    AuroraConfigFileResource("name", "contents", AuroraConfigFileType.APP, "hash")
                )
            )
        val requests = server.executeBlocking(response) {
            val auroraConfig =
                auroraConfigService.getApplicationAuroraConfigFiles("token", "name", "env", "app")
            assertThat(auroraConfig[0].name).isEqualTo("name")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get aurora config failure`() {
        val response = Response(success = false, items = emptyList<AuroraConfig>())
        val requests = server.executeBlocking(response) {
            assertThat { auroraConfigService.getAuroraConfig("token", "auroraConfig", "master") }
                .isFailure().isInstanceOf(SourceSystemException::class)
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Update aurora config file`() {
        val response =
            Response(items = listOf(AuroraConfigFileResource("name", "contents", AuroraConfigFileType.APP, "hash")))
        val requests = server.executeBlocking(response) {
            val fileResource = auroraConfigService.updateAuroraConfigFile(
                "token",
                "auroraConfig",
                "reference",
                "fileName",
                "content",
                "oldHas"
            )
            assertThat(fileResource.name).isEqualTo("name")
        }
        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Add aurora config file`() {
        val response =
            Response(items = listOf(AuroraConfigFileResource("name", "contents", AuroraConfigFileType.APP, "hash")))
        val requests = server.executeBlocking(response) {
            val fileResource = auroraConfigService.addAuroraConfigFile(
                "token",
                "auroraConfig",
                "reference",
                "fileName",
                "content"
            )
            assertThat(fileResource.name).isEqualTo("name")
        }
        assertThat(requests).hasSize(1)
    }
}
