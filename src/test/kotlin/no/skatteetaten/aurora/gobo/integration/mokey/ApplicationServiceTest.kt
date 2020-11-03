package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import no.skatteetaten.aurora.gobo.graphql.PROFILE_WITH_DBH_AND_SKAP
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(PROFILE_WITH_DBH_AND_SKAP)
@SpringBootTest
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:mokey:+:stubs:6565"])
class ApplicationServiceTest : StrubrunnerRepoPropertiesEnabler() {

    @Autowired
    lateinit var applicationService: ApplicationService

    @Test
    fun `Get applications for affiliation`() {
        val applications = runBlocking { applicationService.getApplications(listOf("paas")) }
        assertThat(applications).isNotEmpty()
    }

    @Test
    fun `Get application deployment details for affiliation`() {
        val details = runBlocking {
            applicationService.getApplicationDeploymentDetails("paas", "foo")
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
            applicationService.getApplicationDeployment(listOf(ApplicationDeploymentRef("utv", "gobo")))
        }
        assertThat(applicationDeployments).isNotNull()
    }
}
