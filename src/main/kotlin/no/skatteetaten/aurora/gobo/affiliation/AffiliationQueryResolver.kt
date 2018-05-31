package no.skatteetaten.aurora.gobo.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(val affiliationService: AffiliationService) : GraphQLQueryResolver {

    fun getAffiliations(): List<String>? =
        affiliationService.getAllAffiliations()
}