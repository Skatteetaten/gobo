package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentListDataLoader(
    private val applicationService: ApplicationService
) : KeyDataLoader<String, List<ApplicationDeployment>> {

    // TODO this is not optimal, should batch all the keys
    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<ApplicationDeployment> {
        val resources =
            applicationService.getApplicationDeploymentsForDatabases(context.token(), listOf(key))
        return resources.flatMap { it.applicationDeployments }.map { ApplicationDeployment.create(it) }
    }
}
