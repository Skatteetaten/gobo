package no.skatteetaten.aurora.gobo.service

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.awaitWithRetry
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.boober.responses

@Service
class AffiliationService(
    @TargetService(ServiceTypes.MOKEY) val mokeyWebClient: WebClient,
    val booberWebClient: BooberWebClient
) {

    suspend fun getAllVisibleAffiliations(token: String): List<String> =
        mokeyWebClient
            .get()
            .uri("/api/auth/affiliation")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .awaitWithRetry()

    suspend fun getAllDeployedAffiliations(): List<String> =
        mokeyWebClient
            .get()
            .uri("/api/affiliation")
            .retrieve()
            .awaitWithRetry()

    suspend fun getAllAffiliationNames(): List<String> =
        booberWebClient.get<String>("/v1/auroraconfignames").responses()
}
