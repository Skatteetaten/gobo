package no.skatteetaten.aurora.gobo.service

import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import org.springframework.stereotype.Service

@Service
class ApplicationUpgradeService(
    private val applicationService: ApplicationServiceBlocking,
    private val auroraConfigService: AuroraConfigService
) {

    fun upgrade(token: String, applicationDeploymentId: String, version: String) {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val currentLink = details.link("FilesCurrent")
        val auroraConfigFile = details.link("AuroraConfigFileCurrent")
        val applyLink = details.link("Apply")

        val applicationFile = auroraConfigService.getApplicationFile(token, currentLink)
        auroraConfigService.patch(token, version, auroraConfigFile, applicationFile)
        auroraConfigService.redeploy(token, details, applyLink)
        refreshApplicationDeployment(token, applicationDeploymentId)
    }

    fun refreshApplicationDeployment(token: String, applicationDeploymentId: String): Boolean {
        applicationService.refreshApplicationDeployment(token, RefreshParams(applicationDeploymentId))
        return true
    }

    fun refreshApplicationDeployments(token: String, affiliations: List<String>): Boolean {
        applicationService.refreshApplicationDeployment(token, RefreshParams(affiliations = affiliations))
        return true
    }
}