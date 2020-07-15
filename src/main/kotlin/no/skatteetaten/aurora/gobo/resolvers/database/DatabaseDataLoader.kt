package no.skatteetaten.aurora.gobo.resolvers.database

import no.skatteetaten.aurora.gobo.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.MyGraphQLContext
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class DatabaseDataLoader(
    private val applicationService: ApplicationServiceBlocking
) : MultipleKeysDataLoader<String, List<ApplicationDeployment>> {

    override suspend fun getByKeys(
        keys: Set<String>,
        ctx: MyGraphQLContext
    ): Map<String, Try<List<ApplicationDeployment>>> {
        val resources = applicationService.getApplicationDeploymentsForDatabases("user.token", keys.toList())
        return keys.associateWith { key ->
            val applicationDeployments = resources.filter {
                it.identifier == key
            }.flatMap {
                it.applicationDeployments
            }.map {
                ApplicationDeployment.create(it)
            }

            Try.succeeded(applicationDeployments)
        }
    }
}
