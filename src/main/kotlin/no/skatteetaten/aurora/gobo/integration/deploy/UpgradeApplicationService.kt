package no.skatteetaten.aurora.gobo.integration.deploy

import no.skatteetaten.aurora.gobo.integration.boober.ApplyPayload
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

@Service
class UpgradeApplicationService(
    private val applicationService: ApplicationService,
    private val auroraConfigService: AuroraConfigService
) {

    val logger = LoggerFactory.getLogger(UpgradeApplicationService::class.java)

    fun upgrade(applicationDeploymentId: String, version: String) {

        applicationService.getApplicationDeploymentDetails(applicationDeploymentId)
            .flatMap { details ->
                val currentLink = details.link("FilesCurrent")
                val auroraConfigFile = details.link("AuroraConfigFileCurrent")
                val applyLink = details.link("Apply")

                getApplicationFile(currentLink)
                    .flatMap { applicationFile ->
                        val jsonPatch = createJsonPatch(version)
                        auroraConfigService.patch<AuroraConfigFileResource>(
                            auroraConfigFile,
                            listOf(applicationFile),
                            jsonPatch
                        ).toMono()
                    }.flatMap {
                        val payload =
                            ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
                        auroraConfigService.put<AuroraConfigFileResource>(applyLink, body = payload).toMono()
                    }.flatMap {
                        applicationService.refreshApplicationDeployment(RefreshParams(applicationDeploymentId))
                    }
            }.doOnError {
                logger.error(it.message, it)
            }.block() // step verifier, returnere mono istedet for å kjøre block?
    }

    fun createJsonPatch(version: String): String {
        val jsonPatch = """[{
                      "op": "replace",
                      "path": "/version",
                      "value": $version
                    }]"""
        return jsonPatch
    }

    fun getApplicationFile(it: String): Mono<String> {
        return auroraConfigService.get<AuroraConfigFileResource>(it)
            .filter { it.type == AuroraConfigFileType.APP }
            .map { it.name }
            .toMono()
    }
}