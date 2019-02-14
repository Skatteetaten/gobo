package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.ApplicationDeploymentWithDbResourceBuilder
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.user.User
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class DatabaseSchemaDataLoaderTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val applicationService = ApplicationServiceBlocking(ApplicationService(WebClient.create(url.toString())))
    private val dataLoader = DatabaseSchemaDataLoader(applicationService)

    @Test
    fun `Get ApplicationDeployments by database ids`() {
        val resource = ApplicationDeploymentWithDbResourceBuilder("123").build()
        val request = server.execute(listOf(resource)) {
            val result = dataLoader.getByKeys(User("username", "token"), mutableSetOf("123"))
            assertThat(result).isNotNull()
        }

        assertThat(request.path).isEqualTo("/api/auth/applicationdeploymentbyresource/databases")
    }

//    @Test
//    fun `Handle 404 from ApplicationService`() {
//        server.execute(404, "Not found") {
//            val result = dataLoader.getByKey(User("username", "token"), "applicationDeploymentId")
//            assertThat(result.isFailure).isSameAs(true)
//        }
//    }
//
//    @ParameterizedTest
//    @EnumSource(
//        value = SocketPolicy::class,
//        names = ["STALL_SOCKET_AT_START", "NO_RESPONSE", "DISCONNECT_AT_START"],
//        mode = EnumSource.Mode.EXCLUDE
//    )
//    fun `Handle failure from ApplicationService`(socketPolicy: SocketPolicy) {
//        val failureResponse = MockResponse().apply { this.socketPolicy = socketPolicy }
//        server.execute(failureResponse) {
//            val result =
//                dataLoader.getByKey(User("username", "token"), "applicationDeploymentId")
//            assertThat(result.isFailure).isSameAs(true)
//        }
//    }
}
