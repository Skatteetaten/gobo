package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import org.springframework.beans.factory.annotation.Value
import com.expediagroup.graphql.server.operations.Mutation
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import org.springframework.stereotype.Component
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.toxiproxy.AddKubeToxicOp
import no.skatteetaten.aurora.gobo.integration.toxiproxy.DeleteKubeToxicOp
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicContext
import no.skatteetaten.aurora.gobo.security.ifValidUserToken
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Component
class ToxiProxyToxicMutation(
    val toxiProxyToxicService: ToxiProxyToxicService,
    val kubernetesClient: KubernetesCoroutinesClient,
    @Value("\${gobo.graphql.toxiproxylistenport:}") private val toxiProxyListenPort: Int = 8474
) : Mutation {

    suspend fun addToxiProxyToxic(
        input: AddToxiProxyToxicsInput,
        dfe: DataFetchingEnvironment
    ) {
        dfe.ifValidUserToken {
            val toxiProxyToxicCtx = ToxiProxyToxicContext(
                token = dfe.token(),
                affiliationName = input.affiliation,
                environmentName = input.environment,
                applicationName = input.application,
                toxiProxyListenPort = toxiProxyListenPort
            )
            val addKubeClientOp = AddKubeToxicOp(toxiProxyToxicCtx, input.toxiProxy, kubernetesClient)
            toxiProxyToxicService.manageToxiProxyToxic(toxiProxyToxicCtx, addKubeClientOp)
        }
        return // TODO: spørsmål Hvorfor får jeg feil om jeg definerer at metoden skal være Unit/void?
    }

    suspend fun deleteToxiProxyToxic(
        input: DeleteToxiProxyToxicsInput,
        dfe: DataFetchingEnvironment
    ) {
        dfe.ifValidUserToken {
            val toxiProxyToxicCtx = ToxiProxyToxicContext(
                token = dfe.token(),
                affiliationName = input.affiliation,
                environmentName = input.environment,
                applicationName = input.application,
                toxiProxyListenPort = toxiProxyListenPort
            )
            val deleteKubeClientOp = DeleteKubeToxicOp(toxiProxyToxicCtx, input, kubernetesClient)
            toxiProxyToxicService.manageToxiProxyToxic(toxiProxyToxicCtx, deleteKubeClientOp)
        }
        return
    }
}

data class AddToxiProxyToxicsInput(
    val affiliation: String,
    val environment: String,
    val application: String,
    val toxiProxy: ToxiProxyInput,
)

data class DeleteToxiProxyToxicsInput(
    val affiliation: String,
    val environment: String,
    val application: String,
    val toxiProxyName: String,
    val toxicName: String,
)

data class ToxiProxyInput(val name: String, val toxics: ToxicInput)

@JsonSerialize(using = ToxicInputSerializer::class)
data class ToxicInput(
    val name: String,
    val type: String, // TODO: Kun name bør være mandatory, resten optional ?
    val stream: String,
    val toxicity: Int,
    val attributes: List<ToxicAttributeInput>
)

data class ToxicAttributeInput(val key: String, val value: String)

class ToxicInputSerializer : StdSerializer<ToxicInput>(ToxicInput::class.java) {
    override fun serialize(input: ToxicInput?, json: JsonGenerator?, serializer: SerializerProvider?) {
        if (input != null) {
            json?.let {
                it.writeStartObject()
                it.writeStringField("name", input.name)
                it.writeStringField("type", input.type)
                it.writeStringField("stream", input.stream)
                it.writeNumberField("toxicity", input.toxicity)
                it.writeObjectFieldStart("attributes")
                input.attributes.forEach { attribute ->
                    attribute.value.toIntOrNull()?.let { num ->
                        it.writeNumberField(attribute.key, num)
                    } ?: it.writeStringField(attribute.key, attribute.value)
                }

                it.writeEndObject()
                it.writeEndObject()
            }
        }
    }
}
