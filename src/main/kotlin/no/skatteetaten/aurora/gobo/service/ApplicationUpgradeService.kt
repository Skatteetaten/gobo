package no.skatteetaten.aurora.gobo.service

import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.boober.RedeployResponse
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import org.springframework.stereotype.Service

@Service
class ApplicationUpgradeService(
    private val applicationService: ApplicationService,
    private val auroraConfigService: AuroraConfigService
) {

    suspend fun deployCurrentVersion(token: String, applicationDeploymentId: String): String {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val applyLink = details.link("Apply")?.href ?: throw IllegalArgumentException("No apply link found")
        return auroraConfigService.redeploy(token, details, applyLink).let {
            refreshApplicationDeployment(token, it)
            it.applicationDeploymentId
        }
    }

    suspend fun refreshApplicationDeployment(token: String, applicationDeploymentId: String) {
        applicationService.refreshApplicationDeployment(
            token,
            RefreshParams(applicationDeploymentId)
        )
    }

    suspend fun refreshApplicationDeployment(token: String, redeployResponse: RedeployResponse) {
        applicationService.refreshApplicationDeployment(
            token,
            RefreshParams(redeployResponse.applicationDeploymentId),
            redeployResponse
        )
    }

    suspend fun refreshApplicationDeployments(token: String, affiliations: List<String>) {
        applicationService.refreshApplicationDeployment(
            token,
            RefreshParams(affiliations = affiliations)
        )
    }
}
