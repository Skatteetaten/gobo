package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import org.junit.jupiter.api.Test

class DefaultRegistryMetadataResolverTest {

    private val metadataResolver = DefaultRegistryMetadataResolver("docker-registry.default.svc:5000")

    @Test
    fun `verify metadata for internal registry`() {

        val metadata = metadataResolver.getMetadataForRegistry("docker-registry.default.svc:5000")

        assert(metadata.apiSchema).isEqualTo("http")
        assert(metadata.authenticationMethod).isEqualTo(KUBERNETES_TOKEN)
        assert(metadata.isInternal).isEqualTo(true)
    }

    @Test
    fun `verify metadata for central registry`() {

        val metadata = metadataResolver.getMetadataForRegistry("docker-registry.somesuch.no:5000")

        assert(metadata.apiSchema).isEqualTo("https")
        assert(metadata.authenticationMethod).isEqualTo(NONE)
    }
}