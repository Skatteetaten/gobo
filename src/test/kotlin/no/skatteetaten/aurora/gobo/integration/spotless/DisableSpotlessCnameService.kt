package no.skatteetaten.aurora.gobo.integration.spotless

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresSpotless
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresSpotless::class, SpotlessCnameServiceReactive::class, SpotlessCnameServiceDisabled::class],
    properties = ["integrations.spotless.url=false"]
)
class DisableSpotlessCnameService {

    @Autowired(required = false)
    private var cnameServiceReactive: SpotlessCnameServiceReactive? = null

    @Autowired(required = false)
    private var cnameServiceDisabled: SpotlessCnameServiceDisabled? = null

    @Test
    fun `Disable Spotless given no url configured`() {
        assertThat(cnameServiceReactive).isNull()
        assertThat(cnameServiceDisabled).isNotNull()
    }
}
