package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.resolvers.webseal.WebsealState
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.dataloader.Try
import org.springframework.stereotype.Component

/*
@Component
class DatabaseSchemaListDataLoader(val databaseSchemaServiceReactive: DatabaseServiceReactive) :
    KeyDataLoader<String, List<DatabaseSchemaResource>> {

    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<DatabaseSchemaResource> =
        databaseSchemaServiceReactive.getDatabaseSchemas(key).awaitSingle()
}*/



