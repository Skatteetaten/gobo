package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class PermissionService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    suspend fun getPermission(
        namespace: String,
        token: String
    ): AuroraNamespacePermissions {
        return webClient
            .get()
            .uri("/api/auth/permissions/{namespace}", namespace)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .awaitBody()
    }
}

data class AuroraNamespacePermissions(
    val view: Boolean = true,
    val admin: Boolean = false,
    val namespace: String
)
