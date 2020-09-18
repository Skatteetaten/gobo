package no.skatteetaten.aurora.gobo.service

import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import no.skatteetaten.aurora.gobo.integration.mokey.linkHrefs
import org.springframework.stereotype.Service

@Service
class ApplicationUpgradeService(
    private val applicationService: ApplicationService,
    private val auroraConfigService: AuroraConfigService
) {

    suspend fun upgrade(token: String, applicationDeploymentId: String, version: String) {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val (currentLink, auroraConfigFile, applyLink) = details.linkHrefs(
            "FilesCurrent",
            "AuroraConfigFileCurrent",
            "Apply"
        )

        val applicationFile = auroraConfigService.getApplicationFile(token, currentLink)
        auroraConfigService.patch(token, version, auroraConfigFile, applicationFile)
        auroraConfigService.redeploy(token, details, applyLink)
        refreshApplicationDeployment(token, applicationDeploymentId)
    }

    suspend fun deployCurrentVersion(token: String, applicationDeploymentId: String) {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val applyLink = details.link("Apply")?.href ?: throw IllegalArgumentException("")
        auroraConfigService.redeploy(token, details, applyLink)
        refreshApplicationDeployment(token, applicationDeploymentId)
    }

    fun refreshApplicationDeployment(token: String, applicationDeploymentId: String): Boolean {
        runBlocking { applicationService.refreshApplicationDeployment(token, RefreshParams(applicationDeploymentId)) }
        return true
    }

    fun refreshApplicationDeployments(token: String, affiliations: List<String>): Boolean {
        runBlocking { applicationService.refreshApplicationDeployment(token, RefreshParams(affiliations = affiliations)) }
        return true
    }
}
