package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import uk.q3c.rest.hal.HalResource
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

    @Test
    fun `Get existing link`() {
        val links = Links().apply { add("myLink", "http://localhost") }
        val resource = HalResource(links).linkHref("myLink")
        assertThat(resource).isEqualTo("http://localhost")
    }

    @Test
    fun `Get existing links`() {
        val links = Links().apply {
            add("test1", "http://localhost/1")
            add("test2", "http://localhost/2")
        }
        val (link1, link2) = HalResource(links).linkHrefs("test1", "test2")
        assertThat(link1).isEqualTo("http://localhost/1")
        assertThat(link2).isEqualTo("http://localhost/2")
    }

    @Test
    fun `Throw exception when link is not found`() {
        assertThat {
            HalResource().linkHref("unknownLink")
        }.isNotNull().isFailure()
    }
}
