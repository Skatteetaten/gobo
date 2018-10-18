package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(
    val affiliationService: AffiliationService
) : GraphQLQueryResolver {

    fun getAffiliations(checkForVisibility: Boolean = false, dfe: DataFetchingEnvironment): AffiliationsConnection {
        val affiliationNames = if (checkForVisibility) {
            affiliationService.getAllVisibleAffiliations(dfe.currentUser().token)
        } else {
            affiliationService.getAllAffiliations()
        }
        val edges = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(edges, null)
    }
}