package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import uk.q3c.rest.hal.Links

class HalResourcesKtTest {

    @Test
    fun `Create gobo links`() {
        val links = Links().apply {
            add("test1", "http://localhost/1")
            add("test2", "http://localhost/2")
        }

        val goboLinks = links.toGoboLinks()
        assertThat(goboLinks.size).isEqualTo(2)
        assertThat(goboLinks[0].name).isEqualTo("test1")
        assertThat(goboLinks[0].url.toString()).isEqualTo("http://localhost/1")
        assertThat(goboLinks[1].name).isEqualTo("test2")
        assertThat(goboLinks[1].url.toString()).isEqualTo("http://localhost/2")
    }
}
