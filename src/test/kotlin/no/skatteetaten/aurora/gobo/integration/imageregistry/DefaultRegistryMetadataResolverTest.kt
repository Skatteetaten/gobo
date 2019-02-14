package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import org.junit.jupiter.api.Test

class DefaultRegistryMetadataResolverTest {

    private val metadataResolver = DefaultRegistryMetadataResolver("docker-registry.default.svc:5000")

    @Test
    fun `verify metadata for internal registry`() {

        val metadata = metadataResolver.getMetadataForRegistry("docker-registry.default.svc:5000")

        assertThat(metadata.apiSchema).isEqualTo("http")
        assertThat(metadata.authenticationMethod).isEqualTo(KUBERNETES_TOKEN)
        assertThat(metadata.isInternal).isEqualTo(true)
    }

    @Test
    fun `verify metadata for central registry`() {

        val metadata = metadataResolver.getMetadataForRegistry("docker-registry.somesuch.no:5000")

        assertThat(metadata.apiSchema).isEqualTo("https")
        assertThat(metadata.authenticationMethod).isEqualTo(NONE)
    }

    @Test
    fun `verify metadata for internal IP`() {
        val metadata = metadataResolver.getMetadataForRegistry("127.0.0.1:5000")

        assertThat(metadata.apiSchema).isEqualTo("http")
        assertThat(metadata.authenticationMethod).isEqualTo(KUBERNETES_TOKEN)
        assertThat(metadata.isInternal).isEqualTo(true)
    }
}