package no.skatteetaten.aurora.gobo.graphql.environment

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.environment.EnvironmentStatusType.APPLIED
import no.skatteetaten.aurora.gobo.graphql.environment.EnvironmentStatusType.INACTIVE
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import org.springframework.stereotype.Component

@Component
class EnvironmentStatusDataLoader(
    private val environments: Environments
) : GoboDataLoader<EnvironmentApplication, EnvironmentStatus>() {
    override suspend fun getByKeys(
        keys: Set<EnvironmentApplication>,
        context: GraphQLContext
    ): Map<EnvironmentApplication, EnvironmentStatus> {
        val (requested, applied) = environments.fetchDeploymentStatus(keys, context.token)
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
}
