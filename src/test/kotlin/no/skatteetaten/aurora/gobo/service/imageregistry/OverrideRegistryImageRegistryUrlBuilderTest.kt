package no.skatteetaten.aurora.gobo.service.imageregistry

import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class OverrideRegistryImageRegistryUrlBuilderTest {

    @Test
    fun `Create api url`() {
        val urlBuilder = OverrideRegistryImageRegistryUrlBuilder("http://registry-url")
        val apiUrl = urlBuilder.createApiUrl("", ImageRepo("", "namespace", "name"))
        assertk.assert(apiUrl).isEqualTo("http://registry-url/v2/namespace/name")
    }
}