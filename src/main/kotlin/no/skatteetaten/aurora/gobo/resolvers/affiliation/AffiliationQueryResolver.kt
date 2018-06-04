package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.affiliation.AffiliationService
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(val affiliationService: AffiliationService) : GraphQLQueryResolver {

    fun getAffiliations(): AffiliationsConnection {
        val affiliations = affiliationService.getAllAffiliations().map {
            AffiliationEdge(Affiliation(it, ApplicationsConnection(emptyList(), null)))
        }

        return AffiliationsConnection(affiliations, null)
    }
}