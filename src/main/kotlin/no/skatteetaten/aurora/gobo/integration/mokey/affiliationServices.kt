package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class AffiliationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    fun getAllVisibleAffiliations(token: String): Mono<List<String>> =
        webClient
            .get()
            .uri("/api/auth/affiliation")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .bodyToMono()

    fun getAllAffiliations(): Mono<List<String>> =
        webClient
            .get()
            .uri("/api/affiliation")
            .retrieve()
            .bodyToMono()
}

@Service
class AffiliationServiceBlocking(private val affiliationService: AffiliationService) {

    fun getAllVisibleAffiliations(token: String): List<String> =
        affiliationService.getAllVisibleAffiliations(token).blockNonNullWithTimeout()

    fun getAllAffiliations(): List<String> =
        affiliationService.getAllAffiliations().blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockNonNullWithTimeout() = this.blockNonNullAndHandleError(Duration.ofSeconds(30), "mokey")
}
