package no.skatteetaten.aurora.gobo.service.affiliation

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class AffiliationService(val webClient: WebClient) {

    fun getAllAffiliations(): List<String> =
        webClient
            .get()
            .uri("/api/affiliation")
            .retrieve()
            .bodyToMono<List<String>>()
            .block() ?: emptyList()
}