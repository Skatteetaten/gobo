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

data class AddToxiProxyInput(val name: String, val listen: String, val upstream: String, val enabled: Boolean, val toxics: List<AddToxicInput>)

data class AddToxicInput(
    val name: String,
    val type: String,
    val stream: String,
    val toxicity: Int,
    val attributes: List<AddToxicAttributeInput>
)

@JsonSerialize(using = ToxicAttributeSerializer::class)
data class AddToxicAttributeInput(val key: String, val value: String)

class ToxicAttributeSerializer : StdSerializer<AddToxicAttributeInput>(AddToxicAttributeInput::class.java) {
    override fun serialize(input: AddToxicAttributeInput?, json: JsonGenerator?, serializer: SerializerProvider?) {
        if (input != null) {
            json?.let {
                it.writeStartObject()
                it.writeStringField(input.key, input.value)
                it.writeEndObject()

                print("Setter attributer..")
            }
            print("In ToxicAttributeSerializer: " + json)
        }
    }
}
