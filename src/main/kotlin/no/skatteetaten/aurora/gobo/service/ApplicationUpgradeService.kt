package no.skatteetaten.aurora.gobo.service

import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.JsonPatchOperation
import com.github.fge.jsonpatch.ReplaceOperation
import no.skatteetaten.aurora.gobo.integration.boober.ApplyPayload
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import no.skatteetaten.aurora.gobo.security.UserService
import org.apache.commons.lang3.StringEscapeUtils
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

    fun upgrade(applicationDeploymentId: String, version: String) {
        val token = userService.getToken()
        applicationService.getApplicationDeploymentDetails(applicationDeploymentId)
            .flatMap { details ->
                val currentLink =
                    details.link("FilesCurrent").replace("http://boober", "http://boober-aurora.utv.paas.skead.no")
                val auroraConfigFile = details.link("AuroraConfigFileCurrent")
                    .replace("http://boober", "http://boober-aurora.utv.paas.skead.no")
                val applyLink = details.link("Apply").replace("http://boober", "http://boober-aurora.utv.paas.skead.no")

                getApplicationFile(token, currentLink)
                    .flatMap { applicationFile ->
                        patch(token, version, auroraConfigFile, applicationFile)
                    }.flatMap {
                        redeploy(token, details, applyLink)
                    }.flatMap {
                        refresh(applicationDeploymentId)
                    }
            }.doOnError {
                logger.error(
                    "Exception while upgrading version to $version for applicationDeploymentId $applicationDeploymentId",
                    it
                )
            }.block() // step verifier, returnere mono istedet for å kjøre block?
    }

    private fun getApplicationFile(token: String, it: String): Mono<String> {
        return auroraConfigService.get<AuroraConfigFileResource>(token, it)
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
        val jsonPatch =  """[{
                      "op": "replace",
                      "path": "/version",
                      "value": $version
                    }]"""

        return auroraConfigService.patch<AuroraConfigFileResource>(
            token = token,
            url = auroraConfigFile.replace("{fileName}", applicationFile),
            body = jsonPatch
        ).toMono()
    }

    private fun redeploy(
        token: String,
        details: ApplicationDeploymentDetailsResource,
        applyLink: String
    ): Mono<AuroraConfigFileResource> {
        val payload = ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
        return auroraConfigService.put<AuroraConfigFileResource>(token, applyLink, body = payload).toMono()
    }

    private fun refresh(applicationDeploymentId: String) =
        applicationService.refreshApplicationDeployment(RefreshParams(applicationDeploymentId))
}