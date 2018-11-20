package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class PermissionService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    fun getPermission(
        namespace: String
    ): Mono<AuroraNamespacePermissions> {
        return webClient
            .get()
            .uri("/api/auth/permissions/{namespace}", namespace)
            .retrieve()
            .bodyToMono()
    }
}

data class AuroraNamespacePermissions(
    val view: Boolean = true,
    val admin: Boolean = false,
    val namespace: String
)