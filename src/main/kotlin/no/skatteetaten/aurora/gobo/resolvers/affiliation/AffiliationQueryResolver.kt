package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.affiliation.AffiliationService
import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdge
import org.springframework.stereotype.Component

@Component
class AffiliationQueryResolver(
    val affiliationService: AffiliationService,
    val applicationService: ApplicationService
) : GraphQLQueryResolver {

    fun getAffiliations(): AffiliationsConnection {
        val affiliationNames = affiliationService.getAllAffiliations()
        val applications = applicationService.getApplications(affiliationNames.distinct())
        val affiliations = affiliationNames.map { name ->
            val edges = applications
                .filter { application -> application.affiliation == name }
                .map { createApplicationEdge(it) }
            AffiliationEdge(Affiliation(name, ApplicationsConnection(edges, null)))
        }

        return AffiliationsConnection(affiliations, null)
    }
}