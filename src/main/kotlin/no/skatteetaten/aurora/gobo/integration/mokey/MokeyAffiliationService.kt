package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.awaitWithRetry
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class MokeyAffiliationService(
    @TargetService(ServiceTypes.MOKEY) val WebClient: WebClient
) {

    suspend fun getAllVisibleAffiliations(token: String): List<String> =
        WebClient
            .get()
            .uri("/api/auth/affiliation")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .awaitWithRetry()

    suspend fun getAllDeployedAffiliations(): List<String> =
        WebClient
            .get()
            .uri("/api/affiliation")
            .retrieve()
            .awaitWithRetry()
}
