package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.security.UserService
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(
    val affiliationService: AffiliationService,
    val userService: UserService
) : GraphQLQueryResolver {

    fun getAffiliations(checkForVisibility: Boolean = false): AffiliationsConnection {
        val affiliationNames = if (checkForVisibility) {
            affiliationService.getAllVisiableAffiliations(userService.getToken())
        } else {
            affiliationService.getAllAffiliations()
        }
        val edges = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(edges, null)
    }
}