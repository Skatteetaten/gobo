package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.net.URL

class LinkTest {
    @Test
    fun `Create link with protocol`() {
        val link = Link.create(org.springframework.hateoas.Link("http://localhost", "self"))
        assertThat(link.name).isEqualTo("self")
        assertThat(link.url).isEqualTo(URL("http://localhost"))
    }

    @Test
    fun `Create link without protocol`() {
        val link = Link.create(org.springframework.hateoas.Link("localhost", "self"))
        assertThat(link.name).isEqualTo("self")
        assertThat(link.url).isEqualTo(URL("http://localhost"))
    }
}