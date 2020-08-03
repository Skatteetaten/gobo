package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isSameAs
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.user.User
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClient

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApplicationDeploymentDetailsDataLoaderTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val webClient = ApplicationConfig(500, 500, 500, "")
        .webClientMokey(url.toString(), WebClient.builder())
    private val applicationService = ApplicationServiceBlocking(ApplicationService(webClient))
    private val dataLoader = ApplicationDeploymentDetailsDataLoader(applicationService)

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `Get ApplicationDeploymentDetails by applicationDeploymentId`() {
        val request = server.execute(ApplicationDeploymentDetailsBuilder().build()) {
            val result = dataLoader.getByKey(User("username", "token"), ("applicationDeploymentId"))
            assertThat(result).isNotNull()
        }.first()

        assertThat(request?.path).isNotNull()
            .isEqualTo("/api/auth/applicationdeploymentdetails/applicationDeploymentId")
    }

    @Test
    fun `Handle 404 from ApplicationService`() {
        server.execute(404 to "Not found") {
            val result = dataLoader.getByKey(User("username", "token"), "applicationDeploymentId")
            assertThat(result.isFailure).isSameAs(true)
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = SocketPolicy::class,
        names = ["DISCONNECT_AFTER_REQUEST", "DISCONNECT_DURING_RESPONSE_BODY", "NO_RESPONSE"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `Handle failure from ApplicationService`(socketPolicy: SocketPolicy) {
        val failureResponse = MockResponse().apply { this.socketPolicy = socketPolicy }
        server.execute(failureResponse) {
            val result =
                dataLoader.getByKey(User("username", "token"), "applicationDeploymentId")
            assertThat(result.isFailure).isSameAs(true)
        }
    }
}
