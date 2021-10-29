package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newPod
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import org.springframework.stereotype.Component

@Component
class ToxiProxyDataLoader(
    private val applicationService: ApplicationService,
    private val kubernetesClient: KubernetesCoroutinesClient
) : GoboDataLoader<ToxiProxyId, List<ToxicProxy>>() {

    // Dataloader steps:
    /*
    1. Hent podnavn fra mokey, bruk getApplicationDeploymentDetails (send inn id)
    2. Filtrer ut podnavn som ender på "toxiproxy-sidecar"
    3. gjøre proxy kall mot kubernetes
     */

    override suspend fun getByKeys(keys: Set<ToxiProxyId>, ctx: GoboGraphQLContext): Map<ToxiProxyId, List<ToxicProxy>> {
        return keys.associateWith { id ->
            val applicationDeploymentDetails = applicationService.getApplicationDeploymentDetails(ctx.token(), id.applicationDeploymentId)
            applicationDeploymentDetails.podResources.flatMap { pod ->
                val pods = pod.containers.mapNotNull {
                    container ->
                    if (container.name.endsWith("-toxiproxy-sidecar")) {
                        pod
                    } else {
                        null
                    }
                }

                pods.map {
                    val podName = it.name
                    val deploymentRef = applicationDeploymentDetails.applicationDeploymentCommand.applicationDeploymentRef
                    val application = deploymentRef.application
                    val environment = deploymentRef.environment
                    val affiliation = id.affiliation

                    val pod = newPod {
                        metadata {
                            namespace = "$affiliation-$environment"
                            name = podName
                        }
                    }

                    val json = kubernetesClient.proxyGet<JsonNode>(pod = pod, port = 8474, path = "proxies", token = ctx.token())
                    jacksonObjectMapper().convertValue<ToxicProxy>(json.at("/app"))
                }.map {
                    it
                }
            }
        }
    }
}

data class ToxiProxyId(val applicationDeploymentId: String, val affiliation: String)

data class ToxicProxy(val name: String, val listen: String, val upstream: String, val enabled: Boolean, val toxics: String)

data class Toxic(
    val name: String,
    val type: String,
    val stream: String,
    val toxicity: Boolean,
    @GraphQLIgnore val attributes: JsonNode
) {
    fun attributes(): List<ToxicAttribute> {
        print("Nå er vi i attributes...")
        if (attributes.elements().hasNext()) { // && next().fieldNames().hasNext()) {
            val e = attributes.elements().next().fieldNames().next()
            print(e)
        }
        return emptyList()
    }
}

data class ToxicAttribute(val key: String, val value: String)
