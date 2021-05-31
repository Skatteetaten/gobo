package no.skatteetaten.aurora.gobo.integration.phil

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresPhil
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresPhil::class, PhilServiceReactive::class, PhilServiceDisabled::class],
    properties = ["integrations.phil.url=false"]
)
class DisablePhilServiceTest {

    @Autowired(required = false)
    private var philService: PhilServiceReactive? = null

    @Autowired(required = false)
    private var philServiceDisabled: PhilServiceDisabled? = null

    @Test
    fun `Disable Phil given no url configured`() {
        assertThat(philService).isNull()
        assertThat(philServiceDisabled).isNotNull()
    }
}
