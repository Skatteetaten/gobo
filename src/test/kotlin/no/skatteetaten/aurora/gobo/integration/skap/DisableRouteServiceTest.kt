package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresSkap
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresSkap::class, RouteServiceBlocking::class, RouteServiceDisabled::class],
    properties = ["integrations.skap.url=false"]
)
class DisableRouteServiceTest {
    @Autowired(required = false)
    private var routeServiceBlocking: RouteServiceBlocking? = null

    @Autowired(required = false)
    private var routeServiceDisabled: RouteServiceDisabled? = null

    @Test
    fun `Disable skap given no url configured`() {
        assertThat(routeServiceBlocking).isNull()
        assertThat(routeServiceDisabled).isNotNull()
    }
}
