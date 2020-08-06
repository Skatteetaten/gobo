package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
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

    // FIXME test
    /*
    @Test
    fun `Get ApplicationDeploymentDetails by applicationDeploymentId`() {
        val request = server.execute(ApplicationDeploymentDetailsBuilder().build()) {
            val result = dataLoader.getByKey(User("username", "token"), ("applicationDeploymentId"))
            assertThat(result).isNotNull()
        }.first()

        assertThat(request?.path).isNotNull()
            .isEqualTo("/api/auth/applicationdeploymentdetails/applicationDeploymentId")
    }*/

    // FIXME test
    /*
    @Test
    fun `Handle 404 from ApplicationService`() {
        server.execute(404 to "Not found") {
            val result = dataLoader.getByKey(User("username", "token"), "applicationDeploymentId")
            assertThat(result.isFailure).isSameAs(true)
        }
    }
     */

    // FIXME test
    /*
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
    */
}
