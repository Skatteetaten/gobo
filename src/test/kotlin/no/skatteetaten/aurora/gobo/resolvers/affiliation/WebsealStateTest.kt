package no.skatteetaten.aurora.gobo.resolvers.affiliation

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import org.junit.jupiter.api.Test

class WebsealStateTest {
    private val websealState = WebsealState.create(WebsealStateResourceBuilder().build())

    @Test
    fun `Return all junctions given null propertyNames`() {
        val junctions = websealState.junctions(null)

        assertThat(junctions).hasSize(2)
        assertThat(junctions).hasSize(2)
        assertThat(junctions[0]).contains("\"Active Worker Threads\":\"activeWorkerThreads1\"")
        assertThat(junctions[0]).contains("\"Allow Windows Style URLs\":\"allowWindowsStyleURLs\"")
        assertThat(junctions[1]).contains("\"Active Worker Threads\":\"activeWorkerThreads2\"")
    }

    @Test
    fun `Return filtered junctions given propertyName`() {
        val junctions = websealState.junctions(listOf("Active Worker Threads"))

        assertThat(junctions).hasSize(2)
        assertThat(junctions[0]).doesNotContain("Allow Windows Style URLs")
        assertThat(junctions[1]).doesNotContain("Allow Windows Style URLs")
    }
}
