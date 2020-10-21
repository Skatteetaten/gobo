package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class AffiliationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    suspend fun getAllVisibleAffiliations(token: String): List<String> =
        webClient
            .get()
            .uri("/api/auth/affiliation")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .awaitBody()

    suspend fun getAllAffiliations(): List<String> =
        webClient
            .get()
            .uri("/api/affiliation")
            .retrieve()
            .awaitBody()
}
