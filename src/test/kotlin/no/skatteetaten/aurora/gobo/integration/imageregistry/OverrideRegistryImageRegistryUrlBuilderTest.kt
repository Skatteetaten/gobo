package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class OverrideRegistryImageRegistryUrlBuilderTest {

    @Test
    fun `Create api url`() {
        val urlBuilder = OverrideRegistryImageRegistryUrlBuilder("http://registry-url")
        val apiUrl = urlBuilder.createApiUrl("", ImageRepoDto("", "namespace", "name"))
        assertk.assert(apiUrl).isEqualTo("http://registry-url/v2/namespace/name")
    }
}