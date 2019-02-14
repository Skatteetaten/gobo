package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import org.junit.jupiter.api.Test
import org.springframework.hateoas.Link

class ApplicationDeploymentDetailsResourceTest {
    private val details =
        ApplicationDeploymentDetailsBuilder(resourceLinks = listOf(Link("http://localhost", "myLink"))).build()

    @Test
    fun `Get existing link`() {
        val link = details.link("myLink")
        assertThat(link).isEqualTo("http://localhost")
    }

    @Test
    fun `Throw exception when link is not found`() {
        assertThat {
            details.link("unknownLink")
        }.thrownError {
            hasMessage("Link with rel unknownLink was not found")
        }
    }
}