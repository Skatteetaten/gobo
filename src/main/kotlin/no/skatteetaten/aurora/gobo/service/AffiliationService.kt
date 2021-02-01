package no.skatteetaten.aurora.gobo.service

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.awaitWithRetry
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class AffiliationService(
    @TargetService(ServiceTypes.MOKEY) val mokeyWebClient: WebClient,
    @TargetService(ServiceTypes.BOOBER) val booberWebClient: WebClient
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
        booberWebClient
            .get()
            .uri("/v1/auroraconfignames")
            .retrieve()
            .awaitWithRetry()
}
