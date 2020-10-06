package no.skatteetaten.aurora.gobo.service

import com.fasterxml.jackson.databind.JsonNode
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import no.skatteetaten.aurora.gobo.integration.mokey.linkHrefs
import org.springframework.stereotype.Service

@Service
class ApplicationUpgradeService(
    private val applicationService: ApplicationServiceBlocking,
    private val auroraConfigService: AuroraConfigService
) {

    fun upgrade(token: String, applicationDeploymentId: String, version: String): String {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val (currentLink, auroraConfigFile, applyLink) = details.linkHrefs(
            "FilesCurrent",
            "AuroraConfigFileCurrent",
            "Apply"
        )

        val applicationFile = auroraConfigService.getApplicationFile(token, currentLink)
        auroraConfigService.patch(token, version, auroraConfigFile, applicationFile)
        return auroraConfigService.redeploy(token, details, applyLink).applicationDeploymentId().also {
            refreshApplicationDeployment(token, it)
        }
    }

    fun deployCurrentVersion(token: String, applicationDeploymentId: String): String {
        val details = applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId)
        val applyLink = details.link("Apply")?.href ?: throw IllegalArgumentException("")
        return auroraConfigService.redeploy(token, details, applyLink).applicationDeploymentId().also {
            refreshApplicationDeployment(token, it)
        }
    }

    fun refreshApplicationDeployment(token: String, applicationDeploymentId: String): Boolean {
        applicationService.refreshApplicationDeployment(token, RefreshParams(applicationDeploymentId))
        return true
    }

    fun refreshApplicationDeployments(token: String, affiliations: List<String>): Boolean {
        applicationService.refreshApplicationDeployment(token, RefreshParams(affiliations = affiliations))
        return true
    }

    private fun JsonNode.applicationDeploymentId(): String = this.at("/applicationDeploymentId").textValue()
        ?: throw IllegalStateException("No applicationDeploymentId found in response")
}
