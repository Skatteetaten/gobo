package no.skatteetaten.aurora.gobo.integration.naghub

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.postOrNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

private val logger = KotlinLogging.logger {}

enum class NagHubColor(val hex: String) {
    Red("#FF0000"),
    Green("#008000"),
    Yellow("#FFFF00");

    override fun toString(): String = hex
}

data class DetailedMessage(
    val color: NagHubColor,
    val text: String
)

data class SendMessageRequestNagHub(
    val simpleMessage: String? = null,
    val detailedMessages: List<DetailedMessage>
)

@Service
class NagHubService(
    @TargetService(ServiceTypes.NAGHUB) val webClient: WebClient
) {
    suspend fun sendMessage(channelId: String, simpleMessage: String? = null, vararg message: DetailedMessage) {
        val body = SendMessageRequestNagHub(
            simpleMessage = simpleMessage,
            detailedMessages = message.toList()
        )

        webClient.postOrNull<JsonNode, SendMessageRequestNagHub>(
            body = body,
            uri = "/posts/{channelId}",
            channelId
        ) ?: logger.error { "Unable to send notification through Nag-Hub. Failed message=$message and simpleMessage=$simpleMessage" }
    }
}
