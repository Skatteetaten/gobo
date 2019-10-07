package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration

private val logger = KotlinLogging.logger { }

@Service
class ApplicationDeploymentService(private val booberWebClient: BooberWebClient) {

    fun deleteApplicationDeployment(
        token: String,
        input: DeleteApplicationDeploymentInput
    ): Boolean {
        val response = booberWebClient.post<JsonNode>(
            url = "/v1/applicationdeployment/delete",
            token = token,
            body = mapOf("applicationRefs" to listOf(input))
        ).toMono().blockNonNullWithTimeout()
        logger.debug { "Response from boober delete application deployment: $response" }

        return true
    }

    private fun <T> Mono<T>.blockNonNullWithTimeout() =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "boober")
}

data class DeleteApplicationDeploymentInput(val namespace: String, val name: String)
