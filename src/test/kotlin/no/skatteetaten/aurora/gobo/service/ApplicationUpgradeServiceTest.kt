package no.skatteetaten.aurora.gobo.service

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.message
import com.fasterxml.jackson.databind.node.TextNode
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.AuroraConfigFileBuilder
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.TestObjectMapperConfigurer
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.q3c.rest.hal.Links

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApplicationUpgradeServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val config = ApplicationConfig(500, 500, 500, "", testObjectMapper())
    private val auroraConfigService =
        AuroraConfigService(BooberWebClient("${url}boober", config.webClientBoober(), testObjectMapper()))
    private val applicationService =
        ApplicationServiceBlocking(ApplicationService(config.webClientMokey("${url}mokey")))
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
        val requests = server.execute(
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
        assertThat(requests[1]?.path).isNotNull().isEqualTo("/boober/FilesCurrent")
        assertThat(requests[2]?.path).isNotNull().isEqualTo("/boober/AuroraConfigFileCurrent")
        assertThat(requests[3]?.path).isNotNull().isEqualTo("/boober/Apply")
        assertThat(requests[4]?.path).isNotNull().isEqualTo("/mokey/api/auth/refresh")
    }

    @ParameterizedTest
    @EnumSource(
        value = SocketPolicy::class,
        names = ["DISCONNECT_AFTER_REQUEST", "DISCONNECT_DURING_RESPONSE_BODY", "NO_RESPONSE"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `Handle exception from AuroraConfigService`(socketPolicy: SocketPolicy) {
        val failureResponse = MockResponse().apply { this.socketPolicy = socketPolicy }

        server.execute(failureResponse) {
            assertThat {
                upgradeService.upgrade("token", "applicationDeploymentId", "version")
            }.isNotNull().isFailure().isInstanceOf(SourceSystemException::class)
        }
    }

    @Test
    fun `Handle error response from AuroraConfigService`() {

        server.execute(404 to "Not found") {
            assertThat {
                upgradeService.upgrade("token", "applicationDeploymentId", "version")
            }.isNotNull().isFailure().isInstanceOf(SourceSystemException::class)
                .message().isNotNull().contains("404")
        }
    }

    private fun applicationDeploymentDetailsResponse() =
        ApplicationDeploymentDetailsBuilder(
            resourceLinks = Links().apply {
                add("FilesCurrent", "${url}boober/FilesCurrent")
                add("AuroraConfigFileCurrent", "${url}boober/AuroraConfigFileCurrent")
                add("Apply", "${url}boober/Apply")
            }
        ).build()

    private fun applicationFileResponse() = Response(items = listOf(AuroraConfigFileBuilder().build()))

    private fun patchResponse() = Response(items = listOf(AuroraConfigFileBuilder().build()))

    private fun redeployResponse() = Response(items = listOf(TextNode("{}")))

    private fun refreshResponse() = Response(items = listOf(TextNode("{}")))
}
