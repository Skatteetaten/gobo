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
import no.skatteetaten.aurora.gobo.security.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

@Service
class ApplicationUpgradeService(
    private val applicationService: ApplicationService,
    private val auroraConfigService: AuroraConfigService,
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(ApplicationUpgradeService::class.java)

    fun upgrade(applicationDeploymentId: String, version: String): Mono<Void> {
        val token = userService.getToken()
        return applicationService.getApplicationDeploymentDetails(applicationDeploymentId, token)
            .flatMap { details ->

                val currentLink = details.link("FilesCurrent")
                val auroraConfigFile = details.link("AuroraConfigFileCurrent")
                val applyLink = details.link("Apply")

                getApplicationFile(token, currentLink)
                    .flatMap { applicationFile ->
                        patch(token, version, auroraConfigFile, applicationFile)
                    }.flatMap {
                        redeploy(token, details, applyLink)
                    }.flatMap {
                        refresh(token, applicationDeploymentId)
                    }
            }.doOnError {
                logger.error(
                    "Exception while upgrading version to $version for applicationDeploymentId $applicationDeploymentId",
                    it
                )
            }
    }

    private fun getApplicationFile(token: String, it: String): Mono<String> {
        return auroraConfigService
                .get<AuroraConfigFileResource>(token, it)
            .filter { it.type == AuroraConfigFileType.APP }
            .map { it.name }
            .toMono()
    }

    private fun patch(
        token: String,
        version: String,
        auroraConfigFile: String,
        applicationFile: String
    ): Mono<AuroraConfigFileResource> {
        return auroraConfigService.patch<AuroraConfigFileResource>(
            token = token,
            url = auroraConfigFile.replace("{fileName}", applicationFile), // TODO placeholder cannot contain slash
            body = createVersionPatch(version)
        ).toMono()
    }

    private fun createVersionPatch(version: String): Map<String, String> {
        val jsonPatch = JsonPatch(listOf(ReplaceOperation(JsonPointer("/version"), TextNode(version))))
        return mapOf("content" to jacksonObjectMapper().writeValueAsString(jsonPatch))
    }

    private fun redeploy(
        token: String,
        details: ApplicationDeploymentDetailsResource,
        applyLink: String
    ): Mono<JsonNode> {
        val payload = ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
        return auroraConfigService.put<JsonNode>(token, applyLink, body = payload).toMono()
    }

    private fun refresh(token: String, applicationDeploymentId: String) =
        applicationService.refreshApplicationDeployment(token, RefreshParams(applicationDeploymentId))

    // TODO: Not sure how to test this really, Should we just return the mono and use step verifyer or should we mock the applicationService?
    fun refreshApplicationDeployments(refreshParams: RefreshParams) {
        val token = userService.getToken()
        applicationService.refreshApplicationDeployment(token, refreshParams).block()
    }
}