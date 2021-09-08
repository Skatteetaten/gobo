package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.reactive.function.client.WebClient

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AuroraConfigServiceTest {

    private val server = MockWebServer()
    private val auroraConfigService =
        AuroraConfigService(BooberWebClient(server.url, WebClient.create(), testObjectMapper()))

    @AfterEach
    fun tearDown() {
        kotlin.runCatching {
            server.shutdown()
        }
    }

    @Test
    fun `Get aurora config`() {
        val response = Response(AuroraConfig("name", "ref", "resolvedRef", emptyList()))
        val requests = server.executeBlocking(response) {
            val auroraConfig = auroraConfigService.getAuroraConfig("token", "auroraConfig", "master")
            assertThat(auroraConfig.name).isEqualTo("name")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get aurora config application files`() {
        val response =
            Response(AuroraConfigFileResource("name", "contents", AuroraConfigFileType.APP, "hash"))
        val requests = server.executeBlocking(response) {
            val auroraConfig =
                auroraConfigService.getAuroraConfigFiles("token", "name", "env", "app")
            assertThat(auroraConfig[0].name).isEqualTo("name")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get aurora config failure`() {
        val requests = server.executeBlocking(Response<AuroraConfig>(success = false)) {
            assertThat { auroraConfigService.getAuroraConfig("token", "auroraConfig", "master") }
                .isFailure().isInstanceOf(SourceSystemException::class)
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Update aurora config file`() {
        val response =
            Response(AuroraConfigFileResource("name", "contents", AuroraConfigFileType.APP, "hash"))
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
            Response(AuroraConfigFileResource("name", "contents", AuroraConfigFileType.APP, "hash"))
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
