package no.skatteetaten.aurora.gobo.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.AuroraConfigFileBuilder
import no.skatteetaten.aurora.gobo.graphql.ApplicationRedeployException
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.MokeyIntegrationException
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
    fun `Update application deployment version`() {
        val requests = server.executeBlocking(
            applicationDeploymentDetailsResponse(),
            applicationFileResponse(),
            patchResponse(),
            redeployResponse(),
            refreshResponse()
        ) {
            upgradeService.upgrade("token", "applicationDeploymentId", "version")
        }

        assertThat(requests[0]?.path).isNotNull()
            .isEqualTo("/mokey/api/auth/applicationdeploymentdetails/applicationDeploymentId")
        assertThat(requests[1]?.path).isNotNull().isEqualTo("/boober/v1/auroraconfig/FilesCurrent?reference=master")
        assertThat(requests[2]?.path).isNotNull().isEqualTo("/boober/v1/auroraconfig/AuroraConfigFileCurrent/test/gobo.yaml")
        assertThat(requests[3]?.path).isNotNull().isEqualTo("/boober/v1/auroraconfig/Apply")
        assertThat(requests[4]?.path).isNotNull().isEqualTo("/mokey/api/auth/refresh")
    }

    @Test
    fun `Refresh cache fails during update of application deployment`() {
        val requests = server.executeBlocking(
            200 to applicationDeploymentDetailsResponse(),
            200 to applicationFileResponse(),
            200 to patchResponse(),
            200 to redeployResponse(),
            400 to refreshResponse()
        ) {
            assertThat { upgradeService.upgrade("token", "applicationDeploymentId", "version") }
                .isFailure()
                .isInstanceOf(ApplicationRedeployException::class)
        }
        assertThat(requests.size).isEqualTo(5)
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

    @Test
    fun `Handle error response from AuroraConfigService`() {
        server.executeBlocking(404 to "Not found") {
            assertThat {
                upgradeService.upgrade("token", "applicationDeploymentId", "version")
            }.isNotNull().isFailure().isInstanceOf(MokeyIntegrationException::class).messageContains("not found")
        }
    }

    private fun applicationDeploymentDetailsResponse() =
        ApplicationDeploymentDetailsBuilder(
            resourceLinks = Links().apply {
                add("FilesCurrent", "${url}boober/v1/auroraconfig/FilesCurrent?reference=master")
                add("AuroraConfigFileCurrent", "${url}boober/v1/auroraconfig/AuroraConfigFileCurrent/{fileName}")
                add("Apply", "${url}boober/v1/auroraconfig/Apply")
            }
        ).build()

    private fun applicationFileResponse() =
        Response(items = listOf(AuroraConfigFileBuilder().build()))

    private fun patchResponse() = Response(items = listOf(AuroraConfigFileBuilder().build()))

    private fun redeployResponse() =
        Response(items = listOf(jacksonObjectMapper().readTree("""{ "applicationDeploymentId": "123" }""")))

    private fun refreshResponse() = Response(items = listOf(TextNode("{}")))
}
