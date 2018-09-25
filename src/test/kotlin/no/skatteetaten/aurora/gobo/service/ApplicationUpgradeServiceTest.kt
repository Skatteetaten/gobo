package no.skatteetaten.aurora.gobo.service

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.AuroraConfigFileBuilder
import no.skatteetaten.aurora.gobo.ResponseBuilder
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.enqueueJson
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.security.UserService
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.hateoas.Link

class ApplicationUpgradeServiceTest {

    private val userService = mockk<UserService>().apply {
        every { getToken() } returns "token"
    }

    private val server = MockWebServer()
    private val url = server.url("/")

    private val config = ApplicationConfig("${url}mokey", "${url}unclematt")

    private val applicationService = ApplicationService(config.webClientMokey(), userService)
    private val auroraConfigService = AuroraConfigService("${url}boober", config.webClientBoober())

    private val upgradeService = ApplicationUpgradeService(applicationService, auroraConfigService, userService)

    @Test
    fun `Update application deployment version`() {
        enqueueGetApplicationDeploymentDetails()
        enqueueGetApplicationFile()
        enqueuePatch()
        enqueueRedeploy()
        enqueueRefresh()

        upgradeService.upgrade("applicationDeploymentId", "version")

        val getApplicationDeploymentDetailsRequest = server.takeRequest()
        val getApplicationFileRequest = server.takeRequest()
        val patchRequest = server.takeRequest()
        val redeployRequest = server.takeRequest()
        val refreshRequest = server.takeRequest()
    }

    private fun enqueueGetApplicationDeploymentDetails() {
        val details = ApplicationDeploymentDetailsBuilder(
            resourceLinks = listOf(
                Link("${url}boober/FilesCurrent", "FilesCurrent"),
                Link("${url}boober/AuroraConfigFileCurrent", "AuroraConfigFileCurrent"),
                Link("${url}boober/Apply", "Apply")
            )
        ).build()
        server.enqueueJson(body = details)
    }

    private fun enqueueGetApplicationFile() {
        val file = AuroraConfigFileBuilder().build()
        val response = ResponseBuilder(listOf(file)).build()
        server.enqueueJson(body = response)
    }

    private fun enqueuePatch() {
        val file = AuroraConfigFileBuilder().build()
        val response = ResponseBuilder(listOf(file)).build()
        server.enqueueJson(body = response)
    }

    private fun enqueueRedeploy() {
        server.enqueueJson(body = ResponseBuilder(listOf(TextNode("{}"))).build())
    }

    private fun enqueueRefresh() {
        server.enqueueJson()
    }
}