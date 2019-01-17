package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaQueryResolver(private val databaseSchemaService: DatabaseSchemaServiceBlocking) :
    GraphQLQueryResolver {

    fun databaseSchemas(affiliations: List<String>) = affiliations.flatMap { affiliation ->
        databaseSchemaService.getDatabaseSchemas(affiliation)
            .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
    }
}

@Component
class DatabaseSchemaResolver : GraphQLResolver<DatabaseSchema>