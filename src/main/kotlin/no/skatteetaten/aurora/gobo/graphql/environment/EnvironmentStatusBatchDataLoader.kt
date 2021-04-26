package no.skatteetaten.aurora.gobo.graphql.environment

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

// TODO filter of statuses from Phil
// 1) If status from Phil is failed, use it
// 2) If status from Phil is success, use status from ApplicationDeployment (Mokey)
@Component
class EnvironmentStatusBatchDataLoader(private val applicationService: ApplicationService) :
    GoboDataLoader<EnvironmentApplication, EnvironmentStatus>() {

    override suspend fun getByKeys(keys: Set<EnvironmentApplication>, context: GoboGraphQLContext):
        Map<EnvironmentApplication, EnvironmentStatus> {
            val applicationDeployments = applicationService.getApplicationDeployments(
                keys.map { ApplicationDeploymentRef(it.environment, it.name) }
            )

            return keys.map { app ->
                val ad =
                    applicationDeployments.find { ad ->
                        ad.affiliation == app.affiliation && ad.environment == app.environment && ad.name == app.name
                    }
                app to EnvironmentStatus.create(ad)
            }.toMap()
        }
}
