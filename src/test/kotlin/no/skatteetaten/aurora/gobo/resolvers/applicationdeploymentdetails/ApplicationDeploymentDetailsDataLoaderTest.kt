package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isSameAs
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.user.User
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.reactive.function.client.WebClient

class ApplicationDeploymentDetailsDataLoaderTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val applicationService = ApplicationService(WebClient.create(url.toString()))
    private val dataLoader = ApplicationDeploymentDetailsDataLoader(applicationService)

    @Test
    fun `Get ApplicationDeploymentDetails by applicationDeploymentId`() {
        val request = server.execute(ApplicationDeploymentDetailsBuilder().build()) {
            val result = dataLoader.getByKeys(User("username", "token"), listOf("applicationDeploymentId")).blockFirst()
            assert(result?.get()).isNotNull()
        }

        assert(request.path).isEqualTo("/api/auth/applicationdeploymentdetails/applicationDeploymentId")
    }

    @Test
    fun `Handle 404 from ApplicationService`() {
        server.execute(404, "Not found") {
            val result =
                dataLoader.getByKeys(User("username", "token"), listOf("applicationDeploymentId")).blockFirst()
            assert(result?.isFailure).isSameAs(true)
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = SocketPolicy::class,
        names = ["STALL_SOCKET_AT_START", "NO_RESPONSE", "DISCONNECT_AT_START"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `Handle failure from ApplicationService`(socketPolicy: SocketPolicy) {
        val failureResponse = MockResponse().apply { this.socketPolicy = socketPolicy }
        server.execute(failureResponse) {
            val result =
                dataLoader.getByKeys(User("username", "token"), listOf("applicationDeploymentId")).blockFirst()
            assert(result?.isFailure).isSameAs(true)
        }
    }
}