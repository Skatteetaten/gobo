package no.skatteetaten.aurora.gobo.integration.naghub

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresNagHub
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

class DsiableNagHubServiceTest

@SpringBootTest(
    classes = [RequiresNagHub::class, NagHubServiceReactive::class, NagHubServiceDisabled::class],
    properties = ["integrations.naghub.url=false"]
)
class DisableNagHubServiceTest {

    @Autowired(required = false)
    private var naghubService: NagHubServiceReactive? = null

    @Autowired(required = false)
    private var nagHubServiceDisabled: NagHubServiceDisabled? = null

    @Test
    suspend fun `Disable Nag-Hub given no url configured`() {
        assertThat(naghubService).isNull()
        assertThat(nagHubServiceDisabled).isNotNull()
        assertThat(
            nagHubServiceDisabled?.sendMessage(
                "channeld",
                listOf(DetailedMessage(NagHubColor.Green, "message"))
            )
        ).isNull()
    }
}
