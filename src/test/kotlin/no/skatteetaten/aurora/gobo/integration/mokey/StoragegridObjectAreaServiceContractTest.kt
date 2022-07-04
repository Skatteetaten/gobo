package no.skatteetaten.aurora.gobo.integration.mokey

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import assertk.assertThat
import assertk.assertions.isNotEmpty
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import no.skatteetaten.aurora.gobo.graphql.PROFILE_WITH_DBH_AND_SKAP
import no.skatteetaten.aurora.gobo.security.PsatSecretReader
import no.skatteetaten.aurora.gobo.security.PsatTokenValues
import no.skatteetaten.aurora.gobo.security.SharedSecretReader

@DirtiesContext
@ActiveProfiles(PROFILE_WITH_DBH_AND_SKAP)
@SpringBootTest(
    classes = [
        StrubrunnerRepoPropertiesEnabler.TestConfig::class,
        WebClientAutoConfiguration::class,
        ApplicationConfig::class,
        SharedSecretReader::class,
        PsatTokenValues::class,
        PsatSecretReader::class,
        StorageGridObjectAreasService::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:mokey:+:stubs:6565"])
class StoragegridObjectAreaServiceContractTest : StrubrunnerRepoPropertiesEnabler() {
    @Autowired
    lateinit var storageGridObjectAreasService: StorageGridObjectAreasService

    @Test
    fun `Should get all areas given an affiliation`() {
        val areas = runBlocking { storageGridObjectAreasService.getObjectAreas("foo", "test-token") }
        assertThat(areas).isNotEmpty()
    }
}
