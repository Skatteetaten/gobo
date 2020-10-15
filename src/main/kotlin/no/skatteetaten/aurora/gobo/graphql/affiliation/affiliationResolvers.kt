package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.annotations.GraphQLDescription
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealState
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

data class Affiliation(val name: String) {

    @GraphQLDescription("Get all database schemas for the given affiliation")
    suspend fun databaseSchemas(dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        return dfe.loadMany(name)
    }

    suspend fun websealStates(dfe: DataFetchingEnvironment): List<WebsealState> {
        return dfe.loadMany(name)
    }
}

data class AffiliationEdge(val node: Affiliation) : GoboEdge(node.name)

data class AffiliationsConnection(val edges: List<AffiliationEdge>, val totalCount: Int = edges.size)

@Component
class AffiliationQuery(val affiliationService: AffiliationService) : Query {

    suspend fun affiliations(
        name: String?,
        checkForVisibility: Boolean?,
        dfe: DataFetchingEnvironment
    ): AffiliationsConnection {

        val affiliationNames = name?.let { listOf(name) }
            ?: getAffiliations(checkForVisibility ?: false, dfe.token())

        val affiliations = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(affiliations)
    }

    private suspend fun getAffiliations(checkForVisibility: Boolean, token: String) = if (checkForVisibility) {
        affiliationService.getAllVisibleAffiliations(token)
    } else {
        affiliationService.getAllAffiliations()
    }
}
