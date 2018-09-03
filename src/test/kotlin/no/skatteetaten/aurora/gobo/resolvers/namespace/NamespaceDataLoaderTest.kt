package no.skatteetaten.aurora.gobo.resolvers.namespace

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.ApplicationDeploymentBuilder
import org.junit.jupiter.api.Test

class NamespaceDataLoaderTest {
    private val namespaceDataLoader = NamespaceDataLoader()

    @Test
    fun `Get Namespace by Applications`() {
        val namespaces = namespaceDataLoader.getByKeys(listOf(ApplicationDeploymentBuilder().build()))
        assert(namespaces).hasSize(1)
        assert(namespaces[0].name).isEqualTo("namespaceId")
    }
}