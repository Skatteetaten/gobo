package no.skatteetaten.aurora.gobo.graphql.environment

import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentRefInput
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentStatus
import no.skatteetaten.aurora.gobo.integration.phil.EnvironmentService
import org.springframework.stereotype.Service

fun ApplicationDeploymentResource.status() = EnvironmentStatus.create(this)
fun DeploymentResource.status() = EnvironmentStatus.create(this)

fun EnvironmentApplication.findRunningApplication() = { ad: ApplicationDeploymentResource ->
    ad.affiliation == affiliation && ad.environment == environment && ad.name == name
}

@Service
class Environments(
    private val applicationService: ApplicationService,
    private val environmentService: EnvironmentService,
) {
    fun findDeploymentFromPhil(
        requested: List<DeploymentResource>,
        applied: List<DeploymentResource>
    ): (EnvironmentApplication) -> Pair<EnvironmentApplication, DeploymentResource?> =
        { it to (requested.find(it.findDeployment()) ?: applied.find(it.findDeployment())) }

    private fun EnvironmentApplication.findDeployment(): (DeploymentResource) -> Boolean = {
        it.deploymentRef.affiliation == affiliation &&
            it.deploymentRef.environment == environment &&
            it.deploymentRef.application == name
    }

    suspend fun fetchApplicationStatus(
        applied: List<DeploymentResource>
    ): List<ApplicationDeploymentResource> {
        val mokeyInputs = applied.map {
            ApplicationDeploymentRef(
                it.deploymentRef.environment,
                it.deploymentRef.application
            )
        }

        return applicationService.getApplicationDeployments(mokeyInputs, false)
    }

    suspend fun fetchDeploymentStatus(
        keys: Set<EnvironmentApplication>,
        token: String,
    ): Pair<List<DeploymentResource>, List<DeploymentResource>> {
        val philInputs = keys.map {
            DeploymentRefInput(
                affiliation = it.affiliation,
                environment = it.environment,
                application = it.name,
            )
        }

        return environmentService.getDeploymentStatus(
            philInputs,
            token,
        ).partition { it.status == DeploymentStatus.REQUESTED }
    }
}
