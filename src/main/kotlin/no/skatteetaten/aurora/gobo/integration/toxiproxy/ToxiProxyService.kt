package no.skatteetaten.aurora.gobo.integration.toxiproxy

import org.springframework.stereotype.Service
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newPod
import io.fabric8.kubernetes.api.model.Pod
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.DeleteToxiProxyToxicsInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.ToxiProxyInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.ToxicInput
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

private val logger = KotlinLogging.logger {}

@Service
class ToxiProxyToxicService(
    private val applicationService: ApplicationService
) {

    suspend fun manageToxiProxyToxic(toxiProxyToxicCtx: ToxiProxyToxicContext, kubeClient: KubernetesClientProxy) {
        applicationService.getApplicationDeployments(
            listOf(ApplicationDeploymentRef(toxiProxyToxicCtx.environmentName, toxiProxyToxicCtx.applicationName))
        ).map { resource ->
            val applicationDeploymentDetails =
                applicationService.getApplicationDeploymentDetails(toxiProxyToxicCtx.token, resource.identifier)
            applicationDeploymentDetails.podResources.forEach { pod ->
                if (pod.hasToxiProxySidecar()) {
                    val environment =
                        applicationDeploymentDetails.applicationDeploymentCommand.applicationDeploymentRef.environment
                    val p = newPod {
                        metadata {
                            namespace = "${resource.affiliation}-$environment"
                            name = pod.name
                        }
                    }
                    runCatching {
                        val jsonResponse = kubeClient.callOp(p)
                        logger.debug { "Kubernetes client response: $jsonResponse" }
                        if (jsonResponse?.isFailure == true) throw GoboException("Kubernetes client response is empty")
                    }.onFailure { error: Throwable ->
                        logger.error { "Call to kubernetes client failed: ${error.message} " }
                        throw GoboException("Kubernetes client operation failed on toxoProxy ${kubeClient.toxiProxyName()} ${error.message}")
                    }
                }
            }
        }
    }
}

data class ToxiProxyToxicContext(val token: String, val affiliationName: String, val environmentName: String, val applicationName: String, val toxiProxyListenPort: Int = 8474)

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

abstract class KubernetesClientProxy {
    abstract suspend fun callOp(pod: Pod): Result<JsonNode?>
    open fun toxiProxyName() = ""
}
class AddToxicKubeClient(val ctx: ToxiProxyToxicContext, val toxiProxyInput: ToxiProxyInput, val kubernetesClient: KubernetesCoroutinesClient) : KubernetesClientProxy() {

    override suspend fun callOp(pod: Pod): Result<JsonNode?> {
        val json = kotlin.runCatching {
            kubernetesClient.proxyPost<JsonNode>(
                pod = pod,
                port = ctx.toxiProxyListenPort,
                path = "/proxies/${toxiProxyInput.name}/toxics",
                body = toxiProxyInput.toxics,
                token = ctx.token
            )
        }.onFailure { error: Throwable ->
            logger.error { "${error.message}" }
            throw GoboException("${error.message}")
        }
        return json
    }
    override fun toxiProxyName() = toxiProxyInput.name
}
class DeleteToxicKubeClient(val ctx: ToxiProxyToxicContext, val toxiProxyInput: DeleteToxiProxyToxicsInput, val kubernetesClient: KubernetesCoroutinesClient) : KubernetesClientProxy() {

    override suspend fun callOp(pod: Pod): Result<JsonNode?> {
        val json = kotlin.runCatching {
            kubernetesClient.proxyDelete<JsonNode>(
                pod = pod,
                port = ctx.toxiProxyListenPort,
                path = "/proxies/${toxiProxyInput.toxiProxyName}/toxics/${toxiProxyInput.toxicName}",
                token = ctx.token
            )
        }.onFailure { error: Throwable ->
            logger.error { "${error.message}" }
            throw GoboException("${error.message}")
        }
        return json
    }

    override fun toxiProxyName() = toxiProxyInput.toxiProxyName
}
