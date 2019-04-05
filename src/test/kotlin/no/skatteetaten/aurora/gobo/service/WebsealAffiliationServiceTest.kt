package no.skatteetaten.aurora.gobo.service

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.WebsealStateBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.WebsealServiceBlocking
import org.junit.jupiter.api.Test

class WebsealAffiliationServiceTest {

    private val applicationService = mockk<ApplicationServiceBlocking> {
        every { getApplications(any(), any()) } returns listOf(
            ApplicationResourceBuilder(affiliation = "paas", namespace = "paas").build(),
            ApplicationResourceBuilder(affiliation = "aurora", namespace = "aurora").build()
        )
    }
    private val websealService = mockk<WebsealServiceBlocking> {
        every { getStates() } returns listOf(
            WebsealStateBuilder(namespace = "paas").build(),
            WebsealStateBuilder(namespace = "aurora").build(),
            WebsealStateBuilder(namespace = "test1").build(),
            WebsealStateBuilder(namespace = "test2").build()
        )
    }

    private val websealAffiliationService = WebsealAffiliationService(applicationService, websealService)

    @Test
    fun `Get WebSEAL state for affiliations`() {
        val states = websealAffiliationService.getWebsealState(listOf("paas", "aurora"))
        val namespaces = states.values.flatten().map { it.namespace }

        assertThat(states.size).isEqualTo(2)
        assertThat(states.keys).containsOnly("paas", "aurora")
        assertThat(namespaces).containsOnly("paas", "aurora")
    }
}