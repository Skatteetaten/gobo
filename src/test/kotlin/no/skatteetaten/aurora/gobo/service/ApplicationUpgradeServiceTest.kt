package no.skatteetaten.aurora.gobo.service

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.message
import com.fasterxml.jackson.databind.node.TextNode
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.AuroraConfigFileBuilder
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.hateoas.Link

@MockWebServerTestTag
class ApplicationUpgradeServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val config = ApplicationConfig("${url}mokey", "${url}unclematt", "${url}dbh", 500, 500)

    private val auroraConfigService = AuroraConfigService(BooberWebClient("${url}boober", config.webClientBoober()))
    private val applicationService = ApplicationServiceBlocking(ApplicationService(config.webClientMokey()))
    private val upgradeService = ApplicationUpgradeService(applicationService, auroraConfigService)

    @Test
    fun `Update application deployment version`() {
        val requests = server.execute(
            applicationDeploymentDetailsResponse(),
            applicationFileResponse(),
            patchResponse(),
            redeployResponse(),
            refreshResponse()
        ) {
            upgradeService.upgrade("token", "applicationDeploymentId", "version")
        }

        assertThat(requests[0].path).isEqualTo("/mokey/api/auth/applicationdeploymentdetails/applicationDeploymentId")
        assertThat(requests[1].path).isEqualTo("/boober/FilesCurrent")
        assertThat(requests[2].path).isEqualTo("/boober/AuroraConfigFileCurrent")
        assertThat(requests[3].path).isEqualTo("/boober/Apply")
        assertThat(requests[4].path).isEqualTo("/mokey/api/auth/refresh")
    }

    @ParameterizedTest
    @EnumSource(
        value = SocketPolicy::class,
        names = ["STALL_SOCKET_AT_START", "NO_RESPONSE"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `Handle exception from AuroraConfigService`(socketPolicy: SocketPolicy) {
        val failureResponse = MockResponse().apply { this.socketPolicy = socketPolicy }
        assertThat {
            server.execute(failureResponse) {
                upgradeService.upgrade("token", "applicationDeploymentId", "version")
            }
        }.thrownError {
            server.takeRequest()
            isInstanceOf(SourceSystemException::class)
        }
    }

    @Test
    fun `Handle error response from AuroraConfigService`() {
        assertThat {
            server.execute(404, "Not found") {
                upgradeService.upgrade("token", "applicationDeploymentId", "version")
            }
        }.thrownError {
            server.takeRequest()
            isInstanceOf(SourceSystemException::class)
            message().isNotNull().contains("404")
        }
    }

    private fun applicationDeploymentDetailsResponse() =
        ApplicationDeploymentDetailsBuilder(
            resourceLinks = listOf(
                Link("${url}boober/FilesCurrent", "FilesCurrent"),
                Link("${url}boober/AuroraConfigFileCurrent", "AuroraConfigFileCurrent"),
                Link("${url}boober/Apply", "Apply")
            )
        ).build()

    private fun applicationFileResponse() = Response(items = listOf(AuroraConfigFileBuilder().build()))

    private fun patchResponse() = Response(items = listOf(AuroraConfigFileBuilder().build()))

    private fun redeployResponse() = Response(items = listOf(TextNode("{}")))

    private fun refreshResponse() = Response(items = listOf(TextNode("{}")))
}