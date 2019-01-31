package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assert
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.integration.SpringTestTag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringTestTag
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner
class ApplicationServiceBlockingTest {

    @Autowired
    lateinit var applicationService: ApplicationServiceBlocking

    @Test
    fun `Get applications for affiliation`() {
        val applications = applicationService.getApplications(listOf("paas"))
        assert(applications).isNotEmpty()
    }

    @Test
    fun `Get application deployment details for affiliation`() {
        val details = applicationService.getApplicationDeploymentDetails("paas", "foo")
        assert(details).isNotNull()
    }
}