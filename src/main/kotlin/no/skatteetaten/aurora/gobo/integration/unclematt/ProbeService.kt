package no.skatteetaten.aurora.gobo.integration.unclematt

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class ProbeService(@TargetService(ServiceTypes.UNCLEMATT) val webClient: WebClient) {
    suspend fun probeFirewall(host: String, port: Int): List<ProbeResult> = webClient
        .get()
        .uri {
            it.path("/scan/v1")
                .queryParam("host", host)
                .queryParam("port", port)
                .build()
        }
        .retrieve()
        .awaitBody()
}
