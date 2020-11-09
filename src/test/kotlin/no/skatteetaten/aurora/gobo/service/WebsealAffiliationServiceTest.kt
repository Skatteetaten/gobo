package no.skatteetaten.aurora.gobo.service

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.WebsealServiceReactive
import org.junit.jupiter.api.Test

class WebsealAffiliationServiceTest {

    private val applicationService = mockk<ApplicationService> {
        coEvery { getApplications(any()) } returns listOf(
            ApplicationResourceBuilder(affiliation = "paas", namespace = "paas").build(),
            ApplicationResourceBuilder(affiliation = "aurora", namespace = "aurora").build()
        )
    }
    private val websealService = mockk<WebsealServiceReactive> {
        coEvery { getStates() } returns listOf(
            WebsealStateResourceBuilder(namespace = "paas").build(),
            WebsealStateResourceBuilder(namespace = "aurora").build(),
            WebsealStateResourceBuilder(namespace = "test1").build(),
            WebsealStateResourceBuilder(namespace = "test2").build()
        )
    }

    private val websealAffiliationService = WebsealAffiliationService(applicationService, websealService)

    @Test
    fun `Get WebSEAL state for affiliations`() {
        val states = runBlocking { websealAffiliationService.getWebsealState(listOf("paas", "aurora")) }
        val namespaces = states.values.flatten().map { it.namespace }

        assertThat(states.size).isEqualTo(2)
        assertThat(states.keys).containsOnly("paas", "aurora")
        assertThat(namespaces).containsOnly("paas", "aurora")
    }
}
