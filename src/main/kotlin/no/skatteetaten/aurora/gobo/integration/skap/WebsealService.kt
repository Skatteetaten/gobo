package no.skatteetaten.aurora.gobo.integration.skap

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class WebsealService(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient
) {

    fun getStates(): Mono<List<WebsealState>> =
        webClient
            .get()
            .uri("/webseal/v3")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()
}

@Service
class WebsealServiceBlocking(private val websealService: WebsealService) {
    fun getStates() = websealService.getStates().blockNonNullWithTimeout()

    fun <T> Mono<T>.blockNonNullWithTimeout() = this.blockNonNullAndHandleError(Duration.ofSeconds(30), "skap")
}