package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresSkap
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresSkap::class, JobServiceBlocking::class, JobServiceDisabled::class],
    properties = ["integrations.skap.url=false"]
)
class DisableJobServiceTest {
    @Autowired(required = false)
    private var jobServiceBlocking: JobServiceBlocking? = null

    @Autowired(required = false)
    private var jobServiceDisabled: JobServiceDisabled? = null

    @Test
    fun `Disable skap given no url configured`() {
        assertThat(jobServiceBlocking).isNull()
        assertThat(jobServiceDisabled).isNotNull()
    }
}
