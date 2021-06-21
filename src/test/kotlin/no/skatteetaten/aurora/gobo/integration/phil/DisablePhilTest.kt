package no.skatteetaten.aurora.gobo.integration.phil

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresPhil
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresPhil::class, EnvironmentServiceReactive::class, PhilDisabled::class],
    properties = ["integrations.phil.url=false"]
)
class DisablePhilTest {

    @Autowired(required = false)
    private var philEnvironmentService: EnvironmentServiceReactive? = null

    @Autowired(required = false)
    private var philServiceDisabled: PhilDisabled? = null

    @Test
    fun `Disable Phil given no url configured`() {
        assertThat(philEnvironmentService).isNull()
        assertThat(philServiceDisabled).isNotNull()
    }
}
