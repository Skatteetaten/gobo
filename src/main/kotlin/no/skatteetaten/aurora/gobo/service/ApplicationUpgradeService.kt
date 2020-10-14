package no.skatteetaten.aurora.gobo.service

import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.boober.RedeployResponse
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import no.skatteetaten.aurora.gobo.integration.mokey.linkHrefs
import org.springframework.stereotype.Service

@Service
class ApplicationUpgradeService(
    private val applicationService: ApplicationService,
    private val auroraConfigService: AuroraConfigService
) {

    suspend fun upgrade(token: String, applicationDeploymentId: String, version: String): String {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val (currentLink, auroraConfigFile, applyLink) = details.linkHrefs(
            "FilesCurrent",
            "AuroraConfigFileCurrent",
            "Apply"
        )

        suspend val applicationFile = auroraConfigService.getApplicationFile(token, currentLink)
        auroraConfigService.patch(token, version, auroraConfigFile, applicationFile)
        auroraConfigService.redeploy(token, details, applyLink)
        return auroraConfigService.redeploy(token, details, applyLink).let {
            refreshApplicationDeployment(token, it)
            it.applicationDeploymentId
        }
    }

    suspend fun deployCurrentVersion(token: String, applicationDeploymentId: String) {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val applyLink = details.link("Apply")?.href ?: throw IllegalArgumentException("")
        auroraConfigService.redeploy(token, details, applyLink)
        return auroraConfigService.redeploy(token, details, applyLink).let {
            refreshApplicationDeployment(token, it)
            it.applicationDeploymentId
        }
    }

    fun refreshApplicationDeployment(token: String, applicationDeploymentId: String): Boolean {
        applicationService.refreshApplicationDeployment(
            token,
            RefreshParams(applicationDeploymentId)
        )
        return true
    }

    fun refreshApplicationDeployment(token: String, redeployResponse: RedeployResponse): Boolean {
        applicationService.refreshApplicationDeployment(
            token,
            RefreshParams(redeployResponse.applicationDeploymentId),
            redeployResponse
        )
        return true
    }
}
