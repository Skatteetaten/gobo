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
) {
    companion object {
        fun http(registry: String) = RegistryMetadata(
            registry = registry,
            apiSchema = "http",
            authenticationMethod = KUBERNETES_TOKEN,
            isInternal = true
        )

        fun https(registry: String) = RegistryMetadata(
            registry = registry,
            apiSchema = "https",
            authenticationMethod = NONE,
            isInternal = false
        )
    }
}

interface RegistryMetadataResolver {
    fun getMetadataForRegistry(registry: String): RegistryMetadata
}

@Component
class DefaultRegistryMetadataResolver(@Value("\${gobo.internal-registry.url:docker-registry.default.svc:5000}") val internalRegistryAddress: String) :
    RegistryMetadataResolver {

    private val ipV4WithPortRegex =
        "^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]):[0-9]{1,4}\$".toRegex()

    private fun isInternal(registry: String) =
        registry == internalRegistryAddress || registry.matches(ipV4WithPortRegex)

    override fun getMetadataForRegistry(registry: String) =
        if (isInternal(registry)) {
            RegistryMetadata.http(registry)
        } else {
            RegistryMetadata.https(registry)
        }
}
