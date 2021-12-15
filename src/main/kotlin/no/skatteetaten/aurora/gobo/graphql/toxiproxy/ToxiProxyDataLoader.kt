package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newPod
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import org.springframework.stereotype.Component
import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.token

@Component
class ToxiProxyDataLoader(
    private val applicationService: ApplicationService,
    private val kubernetesClient: KubernetesCoroutinesClient
) : GoboDataLoader<ToxiProxyId, List<ToxiProxy>>() {

    override suspend fun getByKeys(keys: Set<ToxiProxyId>, ctx: GraphQLContext): Map<ToxiProxyId, List<ToxiProxy>> {
        return keys.associateWith { id ->
            val applicationDeploymentDetails = applicationService.getApplicationDeploymentDetails(ctx.token, id.applicationDeploymentId)
            applicationDeploymentDetails.podResources.mapNotNull { pod ->
                if (pod.hasToxiProxySidecar()) {
                    val deploymentRef = applicationDeploymentDetails.applicationDeploymentCommand.applicationDeploymentRef
                    val podInput = newPod {
                        metadata {
                            namespace = "${id.affiliation}-${deploymentRef.environment}"
                            name = pod.name
                        }
                    }
                    val json = kubernetesClient.proxyGet<JsonNode>(pod = podInput, port = 8474, path = "proxies", token = ctx.token)
                    val toxiProxy = jacksonObjectMapper().convertValue<ToxiProxy>(json.at("/app"))
                    toxiProxy.copy(podName = pod.name)
                } else {
                    null
                }
            }
        }
    }
}

data class ToxiProxyId(val applicationDeploymentId: String, val affiliation: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ToxiProxy(val name: String, val listen: String, val upstream: String, val enabled: Boolean, val toxics: List<Toxic>, val podName: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
data class Toxic(
    val name: String,
    val type: String,
    val stream: String,
    val toxicity: Int,
    @GraphQLIgnore
    val attributes: JsonNode
) {
    fun attributes(): List<ToxicAttribute> {
        return jacksonObjectMapper().convertValue<Map<String, String>>(attributes).map { ToxicAttribute(it.key, it.value) }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ToxicAttribute(val key: String, val value: String)
