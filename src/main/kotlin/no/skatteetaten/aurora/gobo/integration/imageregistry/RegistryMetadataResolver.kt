package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import org.springframework.stereotype.Component

enum class AuthenticationMethod { NONE, KUBERNETES_TOKEN }

data class RegistryMetadata(val registry: String, val apiSchema: String, val authenticationMethod: AuthenticationMethod)

interface RegistryMetadataResolver {
    fun getMetadataForRegistry(registry: String): RegistryMetadata
}

@Component
class DefaultRegistryMetadataResolver : RegistryMetadataResolver {
    val ipV4WithPortRegex =
        "^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]):[0-9]{1,4}\$".toRegex()

    override fun getMetadataForRegistry(registry: String): RegistryMetadata {

        val isInternalRegistry = isInternalRegistry(registry)
        return if (isInternalRegistry) RegistryMetadata(registry, "http", KUBERNETES_TOKEN)
        else RegistryMetadata(registry, "https", NONE)
    }

    protected fun isInternalRegistry(registry: String): Boolean {
        return registry.matches(ipV4WithPortRegex)
    }
}
