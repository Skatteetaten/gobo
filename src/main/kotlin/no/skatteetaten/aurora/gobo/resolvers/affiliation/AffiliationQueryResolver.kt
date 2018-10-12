package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.security.UserService
import no.skatteetaten.aurora.gobo.security.UserService.Companion.GUEST_USER_NAME
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(
    val affiliationService: AffiliationService,
    val userService: UserService
) : GraphQLQueryResolver {

    fun getAffiliations(): AffiliationsConnection {
        val affiliationNames=if(userService.getCurrentUser().name== GUEST_USER_NAME) {
            affiliationService.getAllAffiliations()
        } else {
            affiliationService.getAllVisibleAffiliations(userService.getToken())
        }
        val edges = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(edges, null)
    }
}