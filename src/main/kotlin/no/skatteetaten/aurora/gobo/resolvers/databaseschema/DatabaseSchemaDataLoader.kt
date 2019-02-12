package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.MultiKeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaDataLoader(
    private val applicationService: ApplicationServiceBlocking
) : MultiKeyDataLoader<String, List<ApplicationDeployment>> {
    override fun getByKeys(user: User, keys: MutableSet<String>): Map<String, List<ApplicationDeployment>> {
        val resources = applicationService.getApplicationDeploymentsForDatabase(user.token, keys.toList())
        return keys.associate { key ->
            key to resources.filter { resource ->
                resource.databaseId == key
            }.flatMap {
                it.applicationDeployments
            }.map {
                ApplicationDeployment.create(it)
            }
        }
    }
}
