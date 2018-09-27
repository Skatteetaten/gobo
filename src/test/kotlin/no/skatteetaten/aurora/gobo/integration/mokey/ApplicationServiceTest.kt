package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
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

    @Test
    fun `Get application deployment details for affiliation`() {
        val details = applicationService.getApplicationDeploymentDetailsByAffiliations(listOf("paas"))
        assert(details).isNotEmpty()
    }

    @Test
    fun `Verify deserialization of embedded resources works correctly`() {

        val details = applicationService.getApplicationDeploymentDetailsById("appId").block()!!
        val applicationResource = details._embedded["Application"]!!

        println(applicationResource)
        assert(applicationResource.identifier).isEqualTo("appId")
        assert(applicationResource.name).isEqualTo("test")
        assert(applicationResource.applicationDeployments).hasSize(1)
    }
}