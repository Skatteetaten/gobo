package no.skatteetaten.aurora.gobo.service

import assertk.assert
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.AuroraConfigFileBuilder
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.boober.Response
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.security.UserService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Test
import org.springframework.hateoas.Link
import reactor.test.StepVerifier

class ApplicationUpgradeServiceTest {

    private val userService = mockk<UserService>().apply {
        every { getToken() } returns "token"
    }

    private val server = MockWebServer()
    private val url = server.url("/")

    private val config = ApplicationConfig("${url}mokey", "${url}unclematt", 30000, 30000)

    private val applicationService = ApplicationService(config.webClientMokey())
    private val auroraConfigService = AuroraConfigService("${url}boober", config.webClientBoober())

    private val upgradeService = ApplicationUpgradeService(applicationService, auroraConfigService, userService)

    @Test
    fun `Update application deployment version`() {
        val requests = server.execute(
            applicationDeploymentDetailsResponse(),
            applicationFileResponse(),
            patchResponse(),
            redeployResponse(),
            enqueueRefreshResponse()
        ) {
            StepVerifier
                .create(upgradeService.upgrade("applicationDeploymentId", "version"))
                .verifyComplete()
        }

        assert(requests[0].path).isEqualTo("/mokey/api/auth/applicationdeploymentdetails/applicationDeploymentId")
        assert(requests[1].path).isEqualTo("/boober/FilesCurrent")
        assert(requests[2].path).isEqualTo("/boober/AuroraConfigFileCurrent")
        assert(requests[3].path).isEqualTo("/boober/Apply")
        assert(requests[4].path).isEqualTo("/mokey/api/auth/refresh")
    }

    @Test
    fun `Handle IOException from AuroraConfigService`() {
        val failureResponse = MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST }
        server.execute(failureResponse) {
            StepVerifier
                .create(upgradeService.upgrade("applicationDeploymentId", "version"))
                .expectError()
                .verify()
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

    private fun enqueueRefreshResponse() = Response(items = listOf(TextNode("{}")))
}