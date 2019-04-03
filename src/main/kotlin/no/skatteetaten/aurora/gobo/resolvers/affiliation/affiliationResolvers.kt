package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaService
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.WebsealState
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.databaseschema.DatabaseSchema
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.springframework.stereotype.Component

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
    private val databaseSchemaService: DatabaseSchemaService,
    private val websealAffiliationService: WebsealAffiliationService
) : GraphQLResolver<Affiliation> {

    fun databaseSchemas(affiliation: Affiliation, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")
        return databaseSchemaService.getDatabaseSchemas(affiliation.name).map { DatabaseSchema.create(it, affiliation) }
    }

    fun websealStates(affiliation: Affiliation, dfe: DataFetchingEnvironment): List<WebsealState> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL states")
        return websealAffiliationService.getWebsealState(affiliation.name)
    }
}
