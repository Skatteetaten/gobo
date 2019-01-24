package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaQueryResolver(private val databaseSchemaService: DatabaseSchemaServiceBlocking) :
    GraphQLQueryResolver {

    fun databaseSchemas(affiliations: List<String>, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")

        return affiliations.flatMap { affiliation ->
            databaseSchemaService.getDatabaseSchemas(affiliation)
                .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
        }
    }

    fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schema")

        val databaseSchema = databaseSchemaService.getDatabaseSchema(id) ?: return null
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }
}

@Component
class DatabaseSchemaMutation(private val databaseSchemaService: DatabaseSchemaServiceBlocking) :
    GraphQLMutationResolver {
    fun updateDatabaseSchema(input: DatabaseSchemaInput): Boolean =
        (databaseSchemaService.updateDatabaseSchema(input.toSchemaCreationRequest()) != null)
}

@Component
class DatabaseSchemaResolver : GraphQLResolver<DatabaseSchema>