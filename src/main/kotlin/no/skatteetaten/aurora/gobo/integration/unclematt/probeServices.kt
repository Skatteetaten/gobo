package no.skatteetaten.aurora.gobo.integration.unclematt

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ProbeServiceBlocking(private val probeService: ProbeService) {

    fun probeFirewall(host: String, port: Int) =
        probeService.probeFirewall(host, port).blockWithTimeout() ?: emptyList()

    private fun <T> Mono<T>.blockWithTimeout() = this.blockAndHandleError(Duration.ofSeconds(30), "unclematt")
}

@Service
class ProbeService(@TargetService(ServiceTypes.UNCLEMATT) val webClient: WebClient) {

    fun probeFirewall(host: String, port: Int): Mono<List<ProbeResult>> =
        webClient
            .get()
            .uri {
                it.path("/scan/v1")
                    .queryParam("host", host)
                    .queryParam("port", port)
                    .build()
            }
            .retrieve()
            .bodyToMono()
}
