package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import org.springframework.stereotype.Component

enum class AuthenticationMethod { NONE, KUBERNETES_TOKEN }

data class RegistryMetadata(
    val registry: String,
    val apiSchema: String,
    val authenticationMethod: AuthenticationMethod,
    val isInternal: Boolean
)

interface RegistryMetadataResolver {
    fun getMetadataForRegistry(registry: String): RegistryMetadata
}

@Component
class DefaultRegistryMetadataResolver : RegistryMetadataResolver {
    val internalRegistryAddress = "docker-registry.default.svc:5000"

    override fun getMetadataForRegistry(registry: String): RegistryMetadata {

        val isInternalRegistry = isInternalRegistry(registry)
        return if (isInternalRegistry) RegistryMetadata(registry, "http", KUBERNETES_TOKEN, isInternalRegistry)
        else RegistryMetadata(registry, "https", NONE, isInternalRegistry)
    }

    protected fun isInternalRegistry(registry: String): Boolean {
        return registry.equals(internalRegistryAddress)
    }
}
