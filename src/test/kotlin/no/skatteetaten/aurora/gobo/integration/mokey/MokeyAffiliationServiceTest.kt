package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotEmpty
import com.ninjasquad.springmockk.MockkBean
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import no.skatteetaten.aurora.gobo.graphql.PROFILE_WITH_DBH_AND_SKAP
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.gobo.service.AffiliationService
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
    classes = [
        StrubrunnerRepoPropertiesEnabler.TestConfig::class,
        WebClientAutoConfiguration::class,
        ApplicationConfig::class,
        SharedSecretReader::class,
        AffiliationService::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
// TODO points to a specific version due to issues with stubs and webflux in mokey.
//  Must be fixed with an update to the contract tests in mokey
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:mokey:3.3.4:stubs:6565"])
class MokeyAffiliationServiceTest : StrubrunnerRepoPropertiesEnabler() {

    @MockkBean
    lateinit var booberWebClient: BooberWebClient

    @Autowired
    lateinit var affiliationService: AffiliationService

    @Test
    fun `Get affiliations`() {
        val affiliations = runBlocking { affiliationService.getAllDeployedAffiliations() }
        assertThat(affiliations).isNotEmpty()
        assertThat(affiliations).contains("paas")
    }
}
