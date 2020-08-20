package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@Disabled
@ActiveProfiles("with-dbh-and-skap")
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
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
}
