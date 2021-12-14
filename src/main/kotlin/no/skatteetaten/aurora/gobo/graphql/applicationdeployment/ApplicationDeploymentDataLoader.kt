package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDataLoader(private val applicationService: ApplicationService) :
    GoboDataLoader<String, List<ApplicationDeployment>>() {

    override suspend fun getByKeys(
        databaseIds: Set<String>,
        context: GraphQLContext
    ): Map<String, List<ApplicationDeployment>> {
        val resources = applicationService.getApplicationDeploymentsForDatabases(context.token, databaseIds.toList())
        return databaseIds.associateWith { id ->
            resources.filter { it.identifier == id }.flatMap {
                it.applicationDeployments.map { ad -> ApplicationDeployment.create(ad) }
            }
        }
    }
}
