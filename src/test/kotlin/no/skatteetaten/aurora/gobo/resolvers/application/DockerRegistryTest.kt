package no.skatteetaten.aurora.gobo.resolvers.application

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class DockerRegistryTest {
    private val dockerRegistry = DockerRegistry("docker-registry.default.svc:5000")

    @Test
    fun `verify internal registry is recognized as internal`() {
        val isInternal = dockerRegistry.isInternal("docker-registry.default.svc:5000")

        assertThat(isInternal).isTrue()
    }

    @Test
    fun `verify central registry is recognized as not internal`() {
        val isInternal = dockerRegistry.isInternal("docker-registry.somesuch.no:5000")

        assertThat(isInternal).isFalse()
    }

    @Test
    fun `verify internal IP with path is recognized as internal registry`() {
        val isInternal = dockerRegistry.isInternal("127.0.0.1:5000/test123")

        assertThat(isInternal).isTrue()
    }
}
