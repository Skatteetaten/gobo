package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import org.springframework.beans.factory.annotation.Value
import com.expediagroup.graphql.server.operations.Mutation
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.toxiproxy.AddToxicKubeClient
import no.skatteetaten.aurora.gobo.integration.toxiproxy.DeleteToxicKubeClient
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicContext
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxicInputSerializer
import no.skatteetaten.aurora.gobo.integration.toxiproxy.UpdateToxiProxyKubeClient
import no.skatteetaten.aurora.gobo.integration.toxiproxy.UpdateToxicKubeClient
import no.skatteetaten.aurora.gobo.security.ifValidUserToken
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Component
class ToxiProxyToxicMutation(
    val toxiProxyToxicService: ToxiProxyToxicService,
    val kubernetesClient: KubernetesCoroutinesClient,
    @Value("\${gobo.graphql.toxiproxylistenport:8474}") private val toxiProxyListenPort: Int
) : Mutation {

    suspend fun addToxiProxyToxic(
        input: AddOrUpdateToxiProxyInput,
        dfe: DataFetchingEnvironment
    ): ToxiProxyToxicsResponse {
        dfe.ifValidUserToken {
            val toxiProxyToxicCtx = ToxiProxyToxicContext(
                token = dfe.token,
                affiliationName = input.affiliation,
                environmentName = input.environment,
                applicationName = input.application,
                toxiProxyListenPort = toxiProxyListenPort
            )
            val addKubeClientOp = AddToxicKubeClient(toxiProxyToxicCtx, input.toxiProxy, kubernetesClient)
            toxiProxyToxicService.manageToxiProxy(toxiProxyToxicCtx, addKubeClientOp)
        }
        return ToxiProxyToxicsResponse(input.toxiProxy.name, input.toxiProxy.toxics?.name ?: "")
    }

    suspend fun updateToxiProxy(
        input: UpdateToxiProxyInput,
        dfe: DataFetchingEnvironment
    ): ToxiProxyResponse {
        dfe.ifValidUserToken {
            val toxiProxyToxicCtx = ToxiProxyToxicContext(
                token = dfe.token,
                affiliationName = input.affiliation,
                environmentName = input.environment,
                applicationName = input.application,
                toxiProxyListenPort = toxiProxyListenPort
            )
            val clientOp = UpdateToxiProxyKubeClient(toxiProxyToxicCtx, input.toxiProxy, kubernetesClient)
            toxiProxyToxicService.manageToxiProxy(toxiProxyToxicCtx, clientOp)
        }
        return ToxiProxyResponse(input.toxiProxy.name)
    }

    suspend fun updateToxiProxyToxic(
        input: AddOrUpdateToxiProxyInput,
        dfe: DataFetchingEnvironment
    ): ToxiProxyToxicsResponse {
        dfe.ifValidUserToken {
            val toxiProxyToxicCtx = ToxiProxyToxicContext(
                token = dfe.token,
                affiliationName = input.affiliation,
                environmentName = input.environment,
                applicationName = input.application,
                toxiProxyListenPort = toxiProxyListenPort
            )
            val addKubeClientOp = UpdateToxicKubeClient(toxiProxyToxicCtx, input.toxiProxy, kubernetesClient)
            toxiProxyToxicService.manageToxiProxy(toxiProxyToxicCtx, addKubeClientOp)
        }
        return ToxiProxyToxicsResponse(input.toxiProxy.name, input.toxiProxy.toxics?.name ?: "")
    }

    suspend fun deleteToxiProxyToxic(
        input: DeleteToxiProxyToxicsInput,
        dfe: DataFetchingEnvironment
    ): ToxiProxyToxicsResponse {
        dfe.ifValidUserToken {
            val toxiProxyToxicCtx = ToxiProxyToxicContext(
                token = dfe.token,
                affiliationName = input.affiliation,
                environmentName = input.environment,
                applicationName = input.application,
                toxiProxyListenPort = toxiProxyListenPort
            )
            val deleteKubeClientOp = DeleteToxicKubeClient(toxiProxyToxicCtx, input, kubernetesClient)
            toxiProxyToxicService.manageToxiProxy(toxiProxyToxicCtx, deleteKubeClientOp)
        }
        return ToxiProxyToxicsResponse(input.toxiProxyName, input.toxicName)
    }
}

data class ToxiProxyToxicsResponse(val toxiProxyName: String, val toxicName: String)
data class ToxiProxyResponse(val toxiProxyName: String)

data class AddOrUpdateToxiProxyInput(
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

data class UpdateToxiProxyInput(
    val affiliation: String,
    val environment: String,
    val application: String,
    val toxiProxy: ToxiProxyUpdate,
)

data class ToxiProxyUpdate(
    val name: String,
    val listen: String?,
    val upstream: String?,
    val enabled: Boolean?,
)

data class ToxiProxyInput(val name: String, val toxics: ToxicInput)

@JsonSerialize(using = ToxicInputSerializer::class)
data class ToxicInput(
    val name: String,
    val type: String,
    val stream: String,
    val toxicity: Double,
    val attributes: List<ToxicAttributeInput>
)

data class ToxicAttributeInput(val key: String, val value: String)
