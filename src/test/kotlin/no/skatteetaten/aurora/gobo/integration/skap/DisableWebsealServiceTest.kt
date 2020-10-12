package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresSkap
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresSkap::class, WebsealServiceReactive::class, WebsealServiceDisabled::class],
    properties = ["integrations.skap.url=false"]
)
class DisableWebsealServiceTest {
    @Autowired(required = false)
    private var websealService: WebsealServiceReactive? = null

    @Autowired(required = false)
    private var websealServiceDisabled: WebsealServiceDisabled? = null

    @Test
    fun `Disable skap given no url configured`() {
        assertThat(websealService).isNull()
        assertThat(websealServiceDisabled).isNotNull()
    }
}
