package no.skatteetaten.aurora.gobo.graphql.environment

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.environment.EnvironmentStatusType.APPLIED
import no.skatteetaten.aurora.gobo.graphql.environment.EnvironmentStatusType.INACTIVE
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentRefResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class EnvironmentStatusDataLoader(
    private val environments: Environments
) : GoboDataLoader<EnvironmentApplication, EnvironmentStatus>() {
    override suspend fun getByKeys(
        keys: Set<EnvironmentApplication>,
        context: GoboGraphQLContext
    ): Map<EnvironmentApplication, EnvironmentStatus> {
        val (requested, applied) = environments.fetchDeploymentStatus(keys, context.token())
        val applicationDeployments = environments.fetchApplicationStatus(applied)

        return keys
            .map(environments.findDeploymentFromPhil(requested, applied))
            .associate(aggregatedApplicationStatus(applicationDeployments))
    }

    private fun aggregatedApplicationStatus(
        applicationDeployments: List<ApplicationDeploymentResource>
    ): (Pair<EnvironmentApplication, DeploymentResource?>) -> Pair<EnvironmentApplication, EnvironmentStatus> =
        { (app, deployment) ->
            val ad = applicationDeployments.find(app.findRunningApplication())
            val deployStatus = deployment?.status()

            logger.info { "Env Application: ${app.deploymentName()}" }
            logger.info { "Phil status: ${deployment?.deploymentRef?.deploymentName()}" }
            logger.info { "Mokey status: ${ad?.deploymentName()}" }

            app to when (deployStatus?.state) {
                null -> EnvironmentStatus(
                    state = INACTIVE,
                    message = "This application is inactive",
                    details = "Not found in Phil or Mokey"
                )
                APPLIED -> when (val adState = ad?.status()) {
                    null -> deployStatus
                    else -> when (adState.state) {
                        INACTIVE -> deployStatus
                        else -> adState
                    }
                }
                else -> deployStatus
            }
        }

    private fun EnvironmentApplication.deploymentName(): String = "$environment:$affiliation:$name"

    private fun ApplicationDeploymentResource?.deploymentName(): String =
        this?.let { "${it.environment}:${it.affiliation}:${it.name}" } ?: "NO_APP_IN_MOKEY"

    private fun DeploymentRefResource?.deploymentName(): String =
        this?.let { "${it.environment}:${it.affiliation}:${it.application}" } ?: "NO_APP_IN_PHIL"
}
