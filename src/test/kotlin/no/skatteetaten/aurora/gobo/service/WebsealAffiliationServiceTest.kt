package no.skatteetaten.aurora.gobo.service

import assertk.assertThat
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
        every { getApplications(any(), any()) } returns listOf(ApplicationResourceBuilder().build())
    }
    private val websealService = mockk<WebsealServiceBlocking> {
        every { getStates() } returns listOf(
            WebsealStateBuilder(namespace = "namespace").build(),
            WebsealStateBuilder(namespace = "aurora").build()
        )
    }

    private val websealAffiliationService = WebsealAffiliationService(applicationService, websealService)

    @Test
    fun `Get WebSEAL state for affiliation`() {
        val states = websealAffiliationService.getWebsealState("paas")
        assertThat(states.size).isEqualTo(1)
        assertThat(states[0].namespace).isEqualTo("namespace")
    }
}