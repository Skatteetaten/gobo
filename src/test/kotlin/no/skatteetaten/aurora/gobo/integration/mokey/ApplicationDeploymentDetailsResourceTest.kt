package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import org.junit.jupiter.api.Test
import uk.q3c.rest.hal.Links

class ApplicationDeploymentDetailsResourceTest {
    private val details =
        ApplicationDeploymentDetailsBuilder(resourceLinks = Links().apply { add("myLink", "http://localhost") }).build()

    @Test
    fun `Get existing link`() {
        val link = details.linkHref("myLink")
        assertThat(link).isEqualTo("http://localhost")
    }

    @Test
    fun `Throw exception when link is not found`() {
        assertThat {
            details.linkHref("unknownLink")
        }.isNotNull().isFailure()
    }
}
