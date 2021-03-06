package no.skatteetaten.aurora.gobo.integration.naghub

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.RequiresNagHub
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.postOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

private val logger = KotlinLogging.logger {}

enum class NagHubColor(val hex: String) {
    Red("#FF0000"),
    Green("#008000"),
    Yellow("#FFFF00");

    @JsonValue
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

data class NagHubResult(
    val success: Boolean
)

@Service
@ConditionalOnBean(RequiresNagHub::class)
class NagHubServiceReactive(
    @TargetService(ServiceTypes.NAGHUB) val webClient: WebClient
) : NagHubService {
    override suspend fun sendMessage(
        channelId: String,
        messages: List<DetailedMessage>,
        simpleMessage: String?
    ): NagHubResult? {
        val body = SendMessageRequestNagHub(
            simpleMessage = simpleMessage,
            detailedMessages = messages
        )

        val response = webClient.postOrNull<JsonNode>(
            body = body,
            uri = "/api/v1/notify/{channelId}",
            channelId
        )

        response
            ?: logger.error { "Unable to send notification through Nag-Hub. Failed message=$messages and simpleMessage=$simpleMessage" }

        return NagHubResult(
            success = response != null
        )
    }
}

interface NagHubService {
    suspend fun sendMessage(
        channelId: String,
        messages: List<DetailedMessage>,
        simpleMessage: String? = null
    ): NagHubResult? = null
}

@Service
@ConditionalOnMissingBean(RequiresNagHub::class)
class NagHubServiceDisabled : NagHubService
