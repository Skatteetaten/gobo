package no.skatteetaten.aurora.gobo.integration.unclematt

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class ProbeService(
    @TargetService(ServiceTypes.UNCLEMATT) val webClient: WebClient
) {
    private val objectMapper = jacksonObjectMapper()

    init {
        objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
    }

    fun probeFirewall(host: String, port: Int): List<ProbeResult> {
        try {
            val response = webClient
                .get()
                .uri {
                    it.path("/proberesult")
                        .queryParam("host", host)
                        .queryParam("port", port)
                        .build()
                }
                .retrieve()
                .bodyToMono<String>()
                .block() ?: return emptyList()

            val probeResultList = objectMapper.readValue<List<ProbeResult>>(response)

            return probeResultList
        } catch (e: WebClientResponseException) {
            throw SourceSystemException("Failed to invoke firewall scan in the cluster, status:${e.statusCode} message:${e.statusText}", e, e.statusText, "Failed to perfor netdebug operation.")
        }
    }
}
