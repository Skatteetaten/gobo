package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.reactive.awaitFirst
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.resolvers.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.resolvers.token
import org.springframework.stereotype.Component

data class Affiliation(val name: String) {
    fun databaseSchemas(dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        return emptyList()
    }
}

data class Affiliations(val items: List<Affiliation>, val totalCount: Int = items.size)

@Component
class AffiliationQuery(val affiliationService: AffiliationService) : Query {

    suspend fun affiliations(
        affiliation: String?,
        checkForVisibility: Boolean?,
        dfe: DataFetchingEnvironment
    ): Affiliations {

        val affiliationNames = if (affiliation == null) {
            getAffiliations(checkForVisibility ?: false, dfe.token())
        } else {
            listOf(affiliation)
        }

        val affiliations = affiliationNames.map { Affiliation(it) }
        return Affiliations(affiliations)
    }

    private suspend fun getAffiliations(checkForVisibility: Boolean, token: String) = if (checkForVisibility) {
        affiliationService.getAllVisibleAffiliations(token).awaitFirst()
    } else {
        affiliationService.getAllAffiliations().awaitFirst()
    }
}

/*
@Component
class AffiliationQueryResolver(
    val affiliationServiceBlocking: AffiliationServiceBlocking
) : GraphQLQueryResolver {

    fun getAffiliations(
        affiliation: String? = null,
        checkForVisibility: Boolean = false,
        dfe: DataFetchingEnvironment
    ): AffiliationsConnection {
        val affiliationNames = if (affiliation == null) {
            getAffiliations(checkForVisibility, dfe.currentUser().token)
        } else {
            listOf(affiliation)
        }

        val edges = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(edges, null)
    }

    private fun getAffiliations(checkForVisibility: Boolean, token: String) = if (checkForVisibility) {
        affiliationServiceBlocking.getAllVisibleAffiliations(token)
    } else {
        affiliationServiceBlocking.getAllAffiliations()
    }
}

@Component
class AffiliationResolver(
    private val databaseService: DatabaseService,
    private val websealAffiliationService: WebsealAffiliationService
) : GraphQLResolver<Affiliation> {

    fun databaseInstances(affiliation: Affiliation, dfe: DataFetchingEnvironment): List<DatabaseInstance> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database instances")
        return databaseService.getDatabaseInstances().filter {
            it.labels["affiliation"] == affiliation.name
        }.map {
            DatabaseInstance.create(it)
        }
    }

    fun databaseSchemas(affiliation: Affiliation, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")
        return databaseService.getDatabaseSchemas(affiliation.name).map { DatabaseSchema.create(it, affiliation) }
    }

    fun websealStates(affiliation: Affiliation, dfe: DataFetchingEnvironment): CompletableFuture<List<WebsealState>> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL states")
        return dfe.multipleKeysLoader(AffiliationDataLoader::class).load(affiliation.name)
    }
}
*/
