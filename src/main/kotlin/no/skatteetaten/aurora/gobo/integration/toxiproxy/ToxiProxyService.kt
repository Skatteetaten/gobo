package no.skatteetaten.aurora.gobo.integration.toxiproxy

import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newPod
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.AddToxiProxyInput
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Service
class ToxiProxyToxicService(
    private val applicationService: ApplicationService,
    private val kubernetesClient: KubernetesCoroutinesClient
) {

    suspend fun addToxiProxyToxic(toxiProxyToxicCtx: ToxiProxyToxicContext, toxiProxyInput: AddToxiProxyInput) {

        print("input is: " + toxiProxyToxicCtx.affiliationName + " " + toxiProxyToxicCtx.environmentName + " " + toxiProxyToxicCtx.applicationName)
        print("toxic is: " + toJsonNode(toxiProxyInput))
        val applicationDeployments = applicationService.getApplicationDeployments(
            listOf(ApplicationDeploymentRef(toxiProxyToxicCtx.environmentName, toxiProxyToxicCtx.applicationName))
        ).map { resource ->
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
                    val s = jacksonObjectMapper().writeValueAsString(toxiProxyInput.toxics)
                    print("Verdi på toxic: " + s)

                    runCatching {
                        val json = kubernetesClient.proxyPost<JsonNode>(
                            pod = pod,
                            port = 8474,
                            path = "/proxies/${toxiProxyInput.name}/toxics",
                            body = toxiProxyInput.toxics,
                            token = toxiProxyToxicCtx.token
                        )
                        print("Retur fre proxyPOST" + json)
                    }.recoverCatching {
                    }.getOrThrow()
                }.map {
                    it
                }
            }
        }
    }

    private fun toJsonNode(toxics: AddToxiProxyInput) = jacksonObjectMapper().writeValueAsString(toxics.toxics)
}

data class ToxiProxyToxicContext(val token: String, val affiliationName: String, val environmentName: String, val applicationName: String)
