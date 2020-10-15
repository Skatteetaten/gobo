package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class LinkTest {

    @Test
    fun `Create http link with protocol set`() {
        val link = Link.create("self", "http://localhost")
        assertThat(link.url.toString()).isEqualTo("http://localhost")
    }

    @Test
    fun `Create http link without protocol set`() {
        val link = Link.create("self", "localhost")
        assertThat(link.url.toString()).isEqualTo("http://localhost")
    }
}
