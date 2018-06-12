package no.skatteetaten.aurora.gobo.resolvers.affiliation

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.application.ApplicationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AffiliationDataLoaderTest {
    private val applicationService = mockk<ApplicationService>()
    private val affiliationDataLoader = AffiliationDataLoader(applicationService)

    @BeforeEach
    fun setUp() {
        clearMocks(applicationService)
    }

    @Test
    fun `Get Affiliation by affiliationIds`() {
        every { applicationService.getApplications(any()) } returns listOf(ApplicationResourceBuilder().build())

        val affiliations = affiliationDataLoader.getByKeys(listOf("paas", "test", "demo"))
        assert(affiliations).hasSize(3)
        assert(affiliations[0].applications.edges).hasSize(1)
        assert(affiliations[0].applications.edges[0].node.affiliationId).isEqualTo("paas")
        assert(affiliations[1].applications.edges).hasSize(0)
        assert(affiliations[2].applications.edges).hasSize(0)
    }
}