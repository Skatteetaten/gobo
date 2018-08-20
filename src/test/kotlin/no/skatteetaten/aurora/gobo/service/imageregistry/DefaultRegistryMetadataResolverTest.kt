package no.skatteetaten.aurora.gobo.service.imageregistry

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.service.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.service.imageregistry.AuthenticationMethod.NONE
import org.junit.jupiter.api.Test

class DefaultRegistryMetadataResolverTest {

    private val metadataResolver = DefaultRegistryMetadataResolver()

    @Test
    fun `verify metadata for internal registry`() {

        val metadata = metadataResolver.getMetadataForRegistry("172.30.79.77:5000")

        assert(metadata.apiSchema).isEqualTo("http")
        assert(metadata.authenticationMethod).isEqualTo(KUBERNETES_TOKEN)
    }

    @Test
    fun `verify metadata for central registry`() {

        val metadata = metadataResolver.getMetadataForRegistry("docker-registry.somesuch.no:5000")

        assert(metadata.apiSchema).isEqualTo("https")
        assert(metadata.authenticationMethod).isEqualTo(NONE)
    }
}