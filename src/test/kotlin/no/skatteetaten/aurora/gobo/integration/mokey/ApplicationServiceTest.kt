package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import no.skatteetaten.aurora.gobo.graphql.PROFILE_WITH_DBH_AND_SKAP
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@DirtiesContext
@ActiveProfiles(PROFILE_WITH_DBH_AND_SKAP)
@SpringBootTest(
    classes = [WebClientAutoConfiguration::class, ApplicationConfig::class, SharedSecretReader::class, ApplicationService::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:mokey:+:stubs:6565"])
class ApplicationServiceTest : StrubrunnerRepoPropertiesEnabler() {

    @Autowired
    private lateinit var applicationService: ApplicationService

    @Test
    fun `Get applications for affiliation`() {
        val applications = runBlocking { applicationService.getApplications(listOf("paas")) }
        assertThat(applications).isNotEmpty()
    }

    @Test
    fun `Get application deployment details for affiliation`() {
        val details = runBlocking {
            applicationService.getApplicationDeploymentDetails("paas", "123")
        }
        assertThat(details).isNotNull()
    }

    @Test
    fun `Get application deployments for database ids`() {
        val applicationDeployments = runBlocking {
            applicationService.getApplicationDeploymentsForDatabases("", listOf("123", "456"))
        }
        assertThat(applicationDeployments).isNotNull()
    }

    @Test
    fun `Get application deployments for application deployment ref`() {
        val applicationDeployments = runBlocking {
            applicationService.getApplicationDeployments(listOf(ApplicationDeploymentRef("environment", "application")))
        }
        assertThat(applicationDeployments).isNotNull()
    }
}
