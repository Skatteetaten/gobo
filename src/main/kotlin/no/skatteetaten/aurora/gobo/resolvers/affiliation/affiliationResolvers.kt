package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.expediagroup.graphql.spring.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationServiceBlocking
import no.skatteetaten.aurora.gobo.load
import no.skatteetaten.aurora.gobo.loadMany
import no.skatteetaten.aurora.gobo.loadOptional
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.database.DatabaseInstance
import no.skatteetaten.aurora.gobo.resolvers.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.resolvers.multipleKeysLoader
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(
    val affiliationServiceBlocking: AffiliationServiceBlocking
) : Query {

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
) : Query {

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

    suspend fun websealStates(
        affiliation: Affiliation,
        dfe: DataFetchingEnvironment
    ): List<WebsealState> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL states")
        return dfe.loadMany(affiliation.name)
    }
}
