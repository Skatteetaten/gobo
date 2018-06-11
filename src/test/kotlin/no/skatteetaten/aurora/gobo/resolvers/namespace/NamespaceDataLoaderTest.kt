package no.skatteetaten.aurora.gobo.resolvers.namespace

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.application.ApplicationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NamespaceDataLoaderTest {
    private val applicationService = mockk<ApplicationService>()
    private val namespaceDataLoader = NamespaceDataLoader(applicationService)

    @BeforeEach
    fun setUp() {
        clearMocks(applicationService)
    }

    @Test
    fun `Get Namespace by Applications`() {
        every { applicationService.getApplications(any()) } returns listOf(ApplicationResourceBuilder().build())

        val namespaces = namespaceDataLoader.getByKeys(listOf(ApplicationBuilder().build()))

        assert(namespaces).hasSize(1)
        assert(namespaces[0].name).isEqualTo("namespaceId")
    }
}