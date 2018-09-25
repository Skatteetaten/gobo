package no.skatteetaten.aurora.gobo.integration.unclematt

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class ProbeService(
    @TargetService(ServiceTypes.UNCLEMATT) val webClient: WebClient
) {
    private val logger: Logger = LoggerFactory.getLogger(ProbeService::class.java)
    private val exceptionMsg = "Failed to invoke firewall scan in the cluster"
    private val exceptionUserMsg = "Failed to perform netdebug operation"

    fun probeFirewall(host: String, port: Int): List<ProbeResult> {
        try {
            return webClient
                .get()
                .uri {
                    it.path("/scan/v1")
                        .queryParam("host", host)
                        .queryParam("port", port)
                        .build()
                }
                .retrieve()
                .bodyToMono<List<ProbeResult>>()
                .block() ?: return emptyList()
        } catch (e: WebClientResponseException) {
            logger.warn(exceptionUserMsg, e)
            throw SourceSystemException("$exceptionMsg, status:${e.statusCode} message:${e.statusText}", e, e.statusText, exceptionUserMsg)
        } catch (e: Exception) {
            logger.warn(exceptionUserMsg, e)
            throw SourceSystemException(exceptionMsg, e, "", exceptionUserMsg)
        }
    }
}
