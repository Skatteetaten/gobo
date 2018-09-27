package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assert
import assertk.assertions.isNotEmpty
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner
class ApplicationServiceTest {

    @Autowired
    lateinit var applicationService: ApplicationService

    @Test
    fun `Get applications for affiliation`() {
        val applications = applicationService.getApplications(listOf("paas"))
        assert(applications).isNotEmpty()
    }

    @Ignore("change to fetch for one id")
    @Test
    fun `Get application deployment details for affiliation`() {
        val details = applicationService.getApplicationDeploymentDetails("paas")
        assert { details != null }
    }
}