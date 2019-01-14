package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.databaseschema.DatabaseSchema
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(
    val affiliationServiceBlocking: AffiliationServiceBlocking
) : GraphQLQueryResolver {

    fun getAffiliations(checkForVisibility: Boolean = false, dfe: DataFetchingEnvironment): AffiliationsConnection {
        val affiliationNames = if (checkForVisibility) {
            affiliationServiceBlocking.getAllVisibleAffiliations(dfe.currentUser().token)
        } else {
            affiliationServiceBlocking.getAllAffiliations()
        }
        val edges = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(edges, null)
    }
}

@Component
class affiliationResolvers : GraphQLResolver<Affiliation> {

    fun databaseSchemas(affiliation: Affiliation) = emptyList<DatabaseSchema>()
}