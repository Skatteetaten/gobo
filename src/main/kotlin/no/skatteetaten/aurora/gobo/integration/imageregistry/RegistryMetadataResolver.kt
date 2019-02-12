package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import org.springframework.beans.factory.annotation.Value
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
class DefaultRegistryMetadataResolver(@Value("\${gobo.cantus.url}") val internalCantusUrl: String) :
    RegistryMetadataResolver {

    override fun getMetadataForRegistry(registry: String): RegistryMetadata {

        val isInternalRegistry = registry == internalCantusUrl
        return if (isInternalRegistry) RegistryMetadata(registry, "http", KUBERNETES_TOKEN, isInternalRegistry)
        else RegistryMetadata(registry, "https", NONE, isInternalRegistry)
    }
}
