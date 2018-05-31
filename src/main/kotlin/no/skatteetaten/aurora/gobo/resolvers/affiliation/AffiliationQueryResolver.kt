package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.affiliation.AffiliationService
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(val affiliationService: AffiliationService) : GraphQLQueryResolver {

    fun getAffiliations(): List<Affiliation>? =
        affiliationService.getAllAffiliations().map {
            Affiliation(it)
        }
}