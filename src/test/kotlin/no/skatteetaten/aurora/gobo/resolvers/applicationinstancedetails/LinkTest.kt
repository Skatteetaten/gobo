package no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails

import assertk.assert
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.net.URL

class LinkTest {
    @Test
    fun `Create link with protocol`() {
        val link = Link.create(org.springframework.hateoas.Link("http://localhost", "self"))
        assert(link.name).isEqualTo("self")
        assert(link.url).isEqualTo(URL("http://localhost"))
    }

    @Test
    fun `Create link without protocol`() {
        val link = Link.create(org.springframework.hateoas.Link("localhost", "self"))
        assert(link.name).isEqualTo("self")
        assert(link.url).isEqualTo(URL("http://localhost"))
    }
}