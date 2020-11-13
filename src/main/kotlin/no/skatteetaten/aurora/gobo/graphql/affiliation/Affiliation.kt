package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealState

data class Affiliation(val name: String) {

    @GraphQLDescription("Get all database schemas for the given affiliation")
    suspend fun databaseSchemas(dfe: DataFetchingEnvironment): List<DatabaseSchema> = dfe.loadMany(name)

    suspend fun websealStates(dfe: DataFetchingEnvironment): List<WebsealState> = dfe.loadMany(name)
}

data class AffiliationEdge(val node: Affiliation) : GoboEdge(node.name)

data class AffiliationsConnection(val edges: List<AffiliationEdge>, val totalCount: Int = edges.size)
