package no.skatteetaten.aurora.gobo.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.ReplaceOperation
import no.skatteetaten.aurora.gobo.integration.boober.ApplyPayload
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import no.skatteetaten.aurora.gobo.resolvers.blockNonNull
import org.springframework.stereotype.Service
import reactor.core.publisher.toMono

@Service
class ApplicationUpgradeService(
    private val applicationService: ApplicationService,
    private val auroraConfigService: AuroraConfigService
) {

    fun upgrade(applicationDeploymentId: String, version: String, token: String) {
        val details = applicationService.getApplicationDeploymentDetails(applicationDeploymentId, token).blockNonNull()
        val currentLink = details.link("FilesCurrent")
        val auroraConfigFile = details.link("AuroraConfigFileCurrent")
        val applyLink = details.link("Apply")

        val applicationFile = getApplicationFile(token, currentLink)
        patch(token, version, auroraConfigFile, applicationFile)
        redeploy(token, details, applyLink)
        refresh(token, applicationDeploymentId)
    }

    private fun getApplicationFile(token: String, it: String): String {
        return auroraConfigService
            .get<AuroraConfigFileResource>(token, it)
            .filter { it.type == AuroraConfigFileType.APP }
            .map { it.name }
            .toMono()
            .blockNonNull()
    }

    private fun patch(
        token: String,
        version: String,
        auroraConfigFile: String,
        applicationFile: String
    ): AuroraConfigFileResource {
        return auroraConfigService.patch<AuroraConfigFileResource>(
            token = token,
            url = auroraConfigFile.replace("{fileName}", applicationFile), // TODO placeholder cannot contain slash
            body = createVersionPatch(version)
        ).toMono()
            .blockNonNull()
    }

    private fun createVersionPatch(version: String): Map<String, String> {
        val jsonPatch = JsonPatch(listOf(ReplaceOperation(JsonPointer("/version"), TextNode(version))))
        return mapOf("content" to jacksonObjectMapper().writeValueAsString(jsonPatch))
    }

    private fun redeploy(
        token: String,
        details: ApplicationDeploymentDetailsResource,
        applyLink: String
    ): JsonNode {
        val payload = ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
        return auroraConfigService.put<JsonNode>(token, applyLink, body = payload).toMono().blockNonNull()
    }

    private fun refresh(token: String, applicationDeploymentId: String) =
        applicationService.refreshApplicationDeployment(token, RefreshParams(applicationDeploymentId))

    fun refreshApplicationDeployment(applicationDeploymentId: String, token: String): String {
        applicationService.refreshApplicationDeployment(token, RefreshParams(applicationDeploymentId))
        return applicationDeploymentId
    }
}