package no.skatteetaten.aurora.gobo.resolvers.namespace

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.application.StatusResource
import no.skatteetaten.aurora.gobo.application.VersionResource
import no.skatteetaten.aurora.gobo.resolvers.application.Application
import no.skatteetaten.aurora.gobo.resolvers.application.Status
import no.skatteetaten.aurora.gobo.resolvers.application.Version
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
        every { applicationService.getApplications(any()) } returns listOf(
            ApplicationResource(
                "paas",
                "environment",
                "name",
                "namespace",
                StatusResource("code", "comment"),
                VersionResource("deployTag", "auroraVersion")
            )
        )

        val namespaces = namespaceDataLoader.getByKeys(
            listOf(
                Application(
                    "affiliationId",
                    "environment",
                    "namespaceId",
                    "name",
                    Status("code", "comment"),
                    Version("deployTag", "auroraVersion")
                )
            )
        )

        assert(namespaces).hasSize(1)
        assert(namespaces[0].name).isEqualTo("namespaceId")
    }
}