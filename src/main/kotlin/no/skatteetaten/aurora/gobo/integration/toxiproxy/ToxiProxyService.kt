package no.skatteetaten.aurora.gobo.integration.toxiproxy

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newPod
import io.fabric8.kubernetes.api.model.Pod
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.ToxiProxyInput
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Service
class ToxiProxyToxicService(
    private val applicationService: ApplicationService,
    private val kubernetesClient: KubernetesCoroutinesClient
) {

    suspend fun manageToxiProxyToxic(toxiProxyToxicCtx: ToxiProxyToxicContext, toxiProxyInput: ToxiProxyInput, clientOp: KubernetesClientOp) {
        applicationService.getApplicationDeployments(
            listOf(ApplicationDeploymentRef(toxiProxyToxicCtx.environmentName, toxiProxyToxicCtx.applicationName))).map { resource ->
                val applicationDeploymentDetails =
                applicationService.getApplicationDeploymentDetails(toxiProxyToxicCtx.token, resource.identifier)
            applicationDeploymentDetails.podResources.flatMap { pod ->
                val pods = pod.containers.mapNotNull { container ->
                    if (container.name.endsWith("-toxiproxy-sidecar")) {
                        pod
                    } else {
                        null
                    }
                }
                pods.map {
                    val podName = it.name
                    val deploymentRef =
                        applicationDeploymentDetails.applicationDeploymentCommand.applicationDeploymentRef
                    val environment = deploymentRef.environment
                    val affiliation = resource.affiliation

                    val pod = newPod {
                        metadata {
                            namespace = "$affiliation-$environment"
                            name = podName
                        }
                    }
                    runCatching {
                        clientOp.callOp(pod)
                    }.recoverCatching {
                    }.getOrThrow()
                }.map {
                    it
                }
            }
        }
    }
}

data class ToxiProxyToxicContext(val token: String, val affiliationName: String, val environmentName: String, val applicationName: String, val toxiProxyListenPort: Int = 8474)

open class KubernetesClientOp {
    open suspend fun callOp(pod: Pod): JsonNode? {
        return null
    }
}
class AddKubeToxicOp(val ctx: ToxiProxyToxicContext, val toxiProxyInput: ToxiProxyInput, val kubernetesClient: KubernetesCoroutinesClient) : KubernetesClientOp() {

    override suspend fun callOp(pod: Pod): JsonNode? {
        val json = kubernetesClient.proxyPost<JsonNode>(
            pod = pod,
            port = ctx.toxiProxyListenPort,
            path = "/proxies/${toxiProxyInput.name}/toxics",
            body = toxiProxyInput.toxics,
            token = ctx.token
        )
        return json
    }
}
class DeleteKubeToxicOp(val ctx: ToxiProxyToxicContext, val toxiProxyInput: ToxiProxyInput, val kubernetesClient: KubernetesCoroutinesClient) : KubernetesClientOp() {

    override suspend fun callOp(pod: Pod): JsonNode? {
        val json = kubernetesClient.proxyDelete<JsonNode>(
            pod = pod,
            port = ctx.toxiProxyListenPort,
            path = "/proxies/${toxiProxyInput.name}/toxics/${toxiProxyInput.toxics.name}",
            token = ctx.token
        )
        return json
    }
}
