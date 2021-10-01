package no.skatteetaten.aurora.gobo.graphql.environment

import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.environment.EnvironmentStatusType.APPLIED
import no.skatteetaten.aurora.gobo.graphql.environment.EnvironmentStatusType.INACTIVE
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentRefInput
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentStatus
import no.skatteetaten.aurora.gobo.integration.phil.EnvironmentService
import org.springframework.stereotype.Component

@Component
class EnvironmentStatusDataLoader(
    private val applicationService: ApplicationService,
    private val environmentService: EnvironmentService,
) : GoboDataLoader<EnvironmentApplication, EnvironmentStatus>() {
    override suspend fun getByKeys(
        keys: Set<EnvironmentApplication>,
        context: GoboGraphQLContext
    ): Map<EnvironmentApplication, EnvironmentStatus> {
        val (requested, applied) = fetchDeploymentStatus(keys, context.token())
        val applicationDeployments = fetchApplicationStatus(applied)

        return keys
            .map { it to (requested.find(findDeployment(it)) ?: applied.find(findDeployment(it))) }
            .associate { (app, deployment) ->
                val ad = applicationDeployments.find(findRunningApplication(app))
                val deployStatus = deployment?.status()

                app to when (deployStatus?.state) {
                    null -> EnvironmentStatus(
                        state = INACTIVE,
                        message = "This application is inactive",
                        details = "Not found in Phil or Mokey"
                    )
                    APPLIED -> when (val adState = ad?.status()) {
                        null -> deployStatus
                        else -> adState
                    }
                    else -> deployStatus
                }
            }
    }

    private fun ApplicationDeploymentResource.status() = EnvironmentStatus.create(this)
    private fun DeploymentResource.status() = EnvironmentStatus.create(this)

    private fun findRunningApplication(app: EnvironmentApplication) = { ad: ApplicationDeploymentResource ->
        ad.affiliation == app.affiliation && ad.environment == app.environment && ad.name == app.name
    }

    private fun findDeployment(app: EnvironmentApplication): (DeploymentResource) -> Boolean = {
        it.deploymentRef.affiliation == app.affiliation &&
            it.deploymentRef.environment == app.environment &&
            it.deploymentRef.application == app.name
    }

    // TODO This call to mokey needs to uncached so any results after APPLIED from phil are fresh
    private suspend fun fetchApplicationStatus(applied: List<DeploymentResource>): List<ApplicationDeploymentResource> {
        val mokeyInputs = applied.map {
            ApplicationDeploymentRef(
                it.deploymentRef.environment,
                it.deploymentRef.application
            )
        }

        return applicationService.getApplicationDeployments(mokeyInputs)
    }

    private suspend fun fetchDeploymentStatus(
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
