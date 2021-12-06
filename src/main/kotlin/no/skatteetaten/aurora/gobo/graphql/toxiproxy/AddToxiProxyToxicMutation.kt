package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.expediagroup.graphql.server.operations.Mutation
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import org.springframework.stereotype.Component
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicContext
import no.skatteetaten.aurora.gobo.security.ifValidUserToken

// import graphql.schema.DataFetchingEnvironment
// import no.skatteetaten.aurora.gobo.security.checkValidUserToken

@Component
class AddToxiProxyToxicMutation(val toxiProxyToxicService: ToxiProxyToxicService) : Mutation {

    suspend fun addToxiProxyToxic(
        input: AddToxiProxyToxicsInput,
        dfe: DataFetchingEnvironment
    ): String {
        dfe.ifValidUserToken {
            toxiProxyToxicService.addToxiProxyToxic(
                toxiProxyToxicCtx = ToxiProxyToxicContext(
                    token = dfe.token(),
                    affiliationName = input.affiliation,
                    environmentName = input.environment,
                    applicationName = input.application,
                ),
                toxiProxyInput = input.toxiProxy
            )
        }
        return ""
    }
}

data class AddToxiProxyToxicsInput(
    val affiliation: String,
    val environment: String,
    val application: String,
    val toxiProxy: AddToxiProxyInput,
)

data class AddToxiProxyInput(val name: String, val toxics: AddToxicInput)

@JsonSerialize(using = AddToxicInputSerializer::class)
data class AddToxicInput(
    val name: String,
    val type: String,
    val stream: String,
    val toxicity: Int,
    val attributes: List<AddToxicAttributeInput>
)

data class AddToxicAttributeInput(val key: String, val value: String)

class AddToxicInputSerializer : StdSerializer<AddToxicInput>(AddToxicInput::class.java) {
    override fun serialize(input: AddToxicInput?, json: JsonGenerator?, serializer: SerializerProvider?) {
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

                print("Setter attributer..")
            }
            print("In ToxicAttributeSerializer: " + json)
        }
    }
}
