package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class AffiliationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    fun getAllVisiableAffiliations(token: String): List<String> =
        try {
            webClient
                .get()
                .uri("/api/auth/affiliation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token}")
                .retrieve()
                .bodyToMono<List<String>>()
                .block() ?: emptyList()
        } catch (e: WebClientResponseException) {
            throw SourceSystemException(
                "Failed to get affiliations, status:${e.statusCode} message:${e.statusText}",
                e,
                e.statusText,
                "Failed to get affiliations"
            )
        }
    fun getAllAffiliations(): List<String> =
        try {
            webClient
                .get()
                .uri("/api/affiliation")
                .retrieve()
                .bodyToMono<List<String>>()
                .block() ?: emptyList()
        } catch (e: WebClientResponseException) {
            throw SourceSystemException(
                "Failed to get affiliations, status:${e.statusCode} message:${e.statusText}",
                e,
                e.statusText,
                "Failed to get affiliations"
            )
        }
}