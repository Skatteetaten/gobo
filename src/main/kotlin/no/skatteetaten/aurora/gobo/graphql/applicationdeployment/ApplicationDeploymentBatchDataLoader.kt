package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentBatchDataLoader(private val applicationService: ApplicationService) :
    GoboDataLoader<String, List<ApplicationDeployment>>() {

    override suspend fun getByKeys(
        databaseIds: Set<String>,
        context: GoboGraphQLContext
    ): Map<String, List<ApplicationDeployment>> {
        val resources = applicationService.getApplicationDeploymentsForDatabases(context.token(), databaseIds.toList())
        return databaseIds.associateWith { id ->
            resources.filter { it.identifier == id }.flatMap {
                it.applicationDeployments.map { ad -> ApplicationDeployment.create(ad) }
            }
        }
    }
}
