package no.skatteetaten.aurora.gobo.resolvers.database

import kotlinx.coroutines.reactive.awaitSingle
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaListDataLoader(val databaseSchemaServiceReactive: DatabaseServiceReactive) :
    KeyDataLoader<String, List<DatabaseSchemaResource>> {

    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<DatabaseSchemaResource> =
        databaseSchemaServiceReactive.getDatabaseSchemas(key).awaitSingle()
}

/*
@Component
class DatabaseDataLoader(
    private val applicationService: ApplicationServiceBlocking
) : MultipleKeysDataLoader<String, List<ApplicationDeployment>> {
    override fun getByKeys(user: User, keys: MutableSet<String>): Map<String, Try<List<ApplicationDeployment>>> {
        val resources = applicationService.getApplicationDeploymentsForDatabases(user.token, keys.toList())
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
 */
