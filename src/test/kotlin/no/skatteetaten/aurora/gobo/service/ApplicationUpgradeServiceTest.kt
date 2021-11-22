package no.skatteetaten.aurora.gobo.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.TestObjectMapperConfigurer
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.reactive.function.client.WebClient
import uk.q3c.rest.hal.Links

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApplicationUpgradeServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val config = ApplicationConfig(500, 500, 500, "", mockk())
    private val auroraConfigService =
        AuroraConfigService(
            BooberWebClient(
                "${url}boober",
                config.webClientBoober(WebClient.builder()),
                testObjectMapper()
            )
        )
    private val applicationService = ApplicationService(config.webClientMokey("${url}mokey", WebClient.builder()))
    private val upgradeService = ApplicationUpgradeService(applicationService, auroraConfigService)

    @BeforeEach
    fun setUp() {
        TestObjectMapperConfigurer.objectMapper = testObjectMapper()
    }

    @AfterEach
    fun tearDown() {
        TestObjectMapperConfigurer.reset()
        server.shutdown()
    }

    @Test
    fun `Deploy current version`() {
        val requests = server.executeBlocking(
            applicationDeploymentDetailsResponse(),
            redeployResponse(),
            refreshResponse()
        ) {
            upgradeService.deployCurrentVersion("token", "applicationDeploymentId")
        }

        assertThat(requests[0]?.path).isNotNull()
            .isEqualTo("/mokey/api/auth/applicationdeploymentdetails/applicationDeploymentId")
        assertThat(requests[1]?.path).isNotNull().isEqualTo("/boober/v1/auroraconfig/Apply")
    }

    private fun applicationDeploymentDetailsResponse() =
        ApplicationDeploymentDetailsBuilder(
            resourceLinks = Links().apply {
                add("FilesCurrent", "${url}boober/v1/auroraconfig/FilesCurrent?reference=master")
                add("AuroraConfigFileCurrent", "${url}boober/v1/auroraconfig/AuroraConfigFileCurrent/{fileName}")
                add("Apply", "${url}boober/v1/auroraconfig/Apply")
            }
        ).build()

    private fun redeployResponse() =
        Response(items = listOf(jacksonObjectMapper().readTree("""{ "applicationDeploymentId": "123" }""")))

    private fun refreshResponse() = Response(items = listOf(TextNode("{}")))
}
