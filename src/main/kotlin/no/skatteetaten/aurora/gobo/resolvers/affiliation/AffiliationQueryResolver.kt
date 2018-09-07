package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(
    val affiliationService: AffiliationService
) : GraphQLQueryResolver {

    fun getAffiliations(): AffiliationsConnection {
        val affiliationNames = affiliationService.getAllAffiliations()
        val edges = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(edges, null)
    }
}