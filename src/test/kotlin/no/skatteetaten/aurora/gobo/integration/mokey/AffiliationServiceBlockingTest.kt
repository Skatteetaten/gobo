package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assert
import assertk.assertions.contains
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
class AffiliationServiceBlockingTest {

    @Autowired
    lateinit var affiliationServiceBlocking: AffiliationServiceBlocking

    @Test
    fun `Get affiliations`() {
        val affiliations = affiliationServiceBlocking.getAllAffiliations()
        assert(affiliations).isNotEmpty()
        assert(affiliations).contains("paas")
    }
}