package no.skatteetaten.aurora.gobo.integration.gavel

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresGavel
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresGavel::class, CnameServiceReactive::class, CnameServiceDisabled::class],
    properties = ["integrations.gavel.url=false"]
)
class DisableCnameServiceTest {

    @Autowired(required = false)
    private var cnameService: CnameServiceReactive? = null

    @Autowired(required = false)
    private var cnameServiceDisabled: CnameServiceDisabled? = null

    @Test
    fun `Disable Gavel given no url configured`() {
        assertThat(cnameService).isNull()
        assertThat(cnameServiceDisabled).isNotNull()
    }
}
