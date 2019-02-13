package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaDataLoader(
    private val applicationService: ApplicationServiceBlocking
) : MultipleKeysDataLoader<String, List<ApplicationDeployment>> {
    override fun getByKeys(user: User, keys: MutableSet<String>): Map<String, List<ApplicationDeployment>> {
        val resources = applicationService.getApplicationDeploymentsForDatabase(user.token, keys.toList())
        return keys.associate { key ->
            val applicationDeployments = resources.filter { resource ->
                resource.databaseId == key
            }.flatMap {
                it.applicationDeployments
            }.map {
                ApplicationDeployment.create(it)
            }

            key to applicationDeployments
        }
    }
}
