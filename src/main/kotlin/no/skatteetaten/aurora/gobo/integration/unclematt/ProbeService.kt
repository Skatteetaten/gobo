package no.skatteetaten.aurora.gobo.integration.unclematt

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    private val objectMapper = jacksonObjectMapper()
    private val exceptionMsg = "Failed to invoke firewall scan in the cluster"
    private val exceptionUserMsg = "Failed to perform netdebug operation"

    init {
        objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
    }

    fun probeFirewall(host: String, port: Int): List<ProbeResult> {
        try {
            val response = webClient
                .get()
                .uri {
                    it.path("/scan/v1")
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
            logger.warn(exceptionUserMsg, e)
            throw SourceSystemException("$exceptionMsg, status:${e.statusCode} message:${e.statusText}", e, e.statusText, exceptionUserMsg)
        } catch (e: Exception) {
            logger.warn(exceptionUserMsg, e)
            throw SourceSystemException(exceptionMsg, e, "", exceptionUserMsg)
        }
    }
}
