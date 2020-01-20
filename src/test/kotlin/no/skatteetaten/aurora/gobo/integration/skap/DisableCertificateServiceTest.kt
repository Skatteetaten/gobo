package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresSkap
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresSkap::class, CertificateServiceBlocking::class, CertificateServiceDisabled::class],
    properties = ["integrations.skap.url=false"]
)
class DisableCertificateServiceTest {
    @Autowired(required = false)
    private var certificateServiceBlocking: CertificateServiceBlocking? = null

    @Autowired(required = false)
    private var certificateServiceDisabled: CertificateServiceDisabled? = null

    @Test
    fun `Disable skap given no url configured`() {
        assertThat(certificateServiceBlocking).isNull()
        assertThat(certificateServiceDisabled).isNotNull()
    }
}
