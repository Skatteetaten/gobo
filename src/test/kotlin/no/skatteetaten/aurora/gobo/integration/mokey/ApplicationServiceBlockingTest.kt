package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentRef
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("with-dbh-and-skap")
@SpringBootTest
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:mokey:+:stubs:6565"])
class ApplicationServiceBlockingTest : StrubrunnerRepoPropertiesEnabler() {

    @Autowired
    lateinit var applicationService: ApplicationServiceBlocking

    @Test
    fun `Get applications for affiliation`() {
        val applications = applicationService.getApplications(listOf("paas"))
        assertThat(applications).isNotEmpty()
    }

    @Test
    fun `Get application deployment details for affiliation`() {
        val details = applicationService.getApplicationDeploymentDetails("paas", "foo")
        assertThat(details).isNotNull()
    }

    @Test
    fun `Get application deployments for database ids`() {
        val applicationDeployments = applicationService.getApplicationDeploymentsForDatabases("", listOf("123", "456"))
        assertThat(applicationDeployments).isNotNull()
    }

    @Test
    fun `Get application deployments for application deployment ref`() {
        val applicationDeployments = applicationService.getApplicationDeployment(listOf(ApplicationDeploymentRef("utv", "gobo")))
        assertThat(applicationDeployments).isNotNull()
    }
}
