package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ApplicationDeploymentWithDbResourceBuilder
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.user.User
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@MockWebServerTestTag
class DatabaseSchemaDataLoaderTest {

    private val server = MockWebServer()
    private val dbhUrl = server.url("/").toString()
    private val applicationConfig = ApplicationConfig(
        connectionTimeout = 100,
        readTimeout = 100,
        applicationName = ""
    )
    private val dbhClient = applicationConfig.webClientDbh(dbhUrl)
    private val applicationService = ApplicationServiceBlocking(ApplicationService(dbhClient))
    private val dataLoader = DatabaseSchemaDataLoader(applicationService)

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `Get ApplicationDeployments by database ids`() {
        val resource1 = ApplicationDeploymentWithDbResourceBuilder("123").build()
        val resource2 = ApplicationDeploymentWithDbResourceBuilder("456").build()
        val request = server.execute(listOf(resource1, resource2)) {
            val result = dataLoader.getByKeys(User("username", "token"), mutableSetOf("123", "456"))
            assertThat(result["123"]?.get()?.size).isEqualTo(1)
        }

        assertThat(request.path).isEqualTo("/api/auth/applicationdeploymentbyresource/databases")
    }
}
