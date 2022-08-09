package no.skatteetaten.aurora.gobo.integration.herkimer

import assertk.assertThat
import assertk.assertions.isTrue
import com.ninjasquad.springmockk.MockkBean
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.RequiresHerkimer
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import no.skatteetaten.aurora.gobo.TestConfig
import no.skatteetaten.aurora.gobo.graphql.credentials.PostgresHerkimerDatabaseInstance
import no.skatteetaten.aurora.gobo.security.PsatSecretReader
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.test.annotation.DirtiesContext

@DirtiesContext
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [RequiresHerkimer::class, TestConfig::class, ApplicationConfig::class, HerkimerServiceReactive::class, SharedSecretReader::class]
)
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:herkimer:+:stubs:6570"])
class HerkimerServiceContractTest : StrubrunnerRepoPropertiesEnabler() {

    @MockkBean
    private lateinit var psatSecretReader: PsatSecretReader

    @Autowired
    private lateinit var herkimerService: HerkimerServiceReactive

    @Test
    fun `verify is able to register resource and claim`() {
        val result = runBlocking {
            herkimerService.registerResourceAndClaim(
                RegisterResourceAndClaimCommand(
                    ownerId = "12345",
                    credentials = PostgresHerkimerDatabaseInstance("instance", "host", 5432, "admin", "pass", "aurora"),
                    resourceName = "resourceName",
                    claimName = "claimName",
                    resourceKind = ResourceKind.PostgresDatabaseInstance
                )
            )
        }
        assertThat(result.success).isTrue()
    }
}
