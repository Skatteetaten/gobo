package no.skatteetaten.aurora.gobo.integration.toxiproxy

import org.springframework.stereotype.Service
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.AddToxiProxyInput
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Service
class ToxiProxyToxicService(
    private val applicationService: ApplicationService,
    private val kubernetesClient: KubernetesCoroutinesClient) {

    suspend fun addToxiProxyToxic(toxiProxyToxicCtx: ToxiProxyToxicContext, toxic: AddToxiProxyInput) {

        print("input is: " + toxiProxyToxicCtx.affiliationName + " " + toxiProxyToxicCtx.environmentName + " " + toxiProxyToxicCtx.applicationName)
        val applicationDeployments = applicationService.getApplicationDeployments {
            mapOf(ApplicationDeploymentRef(toxiProxyToxicCtx.environmentName, toxiProxyToxicCtx.applicationName)
            )
        }
        // innhold se:  suspend fun addAuroraConfigFile(
    }

    private fun mapOf(pair: ApplicationDeploymentRef) {
    }
}

data class ToxiProxyToxicContext(val token: String, val affiliationName: String, val environmentName: String, val applicationName: String)
