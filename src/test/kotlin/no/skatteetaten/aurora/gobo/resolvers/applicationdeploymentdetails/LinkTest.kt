package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class LinkTest {

    @Test
    fun `Create http link with protocol set`() {
        val link = Link.Create("self", "http://localhost")
        assertThat(link.url.toString()).isEqualTo("http://localhost")
    }

    @Test
    fun `Create http link without protocol set`() {
        val link = Link.Create("self", "localhost")
        assertThat(link.url.toString()).isEqualTo("http://localhost")
    }
}
