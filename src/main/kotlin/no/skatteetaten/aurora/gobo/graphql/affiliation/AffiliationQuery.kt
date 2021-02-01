package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.service.AffiliationService
import org.springframework.stereotype.Component

@Component
class AffiliationQuery(val affiliationService: AffiliationService) : Query {

    suspend fun affiliations(
        name: String?,
        checkForVisibility: Boolean?,
        includeUndeployed: Boolean?,
        dfe: DataFetchingEnvironment
    ): AffiliationsConnection {

        val affiliationNames = name?.let { listOf(name) }
            ?: getAffiliations(checkForVisibility ?: false, includeUndeployed ?: false, dfe)

        val affiliations = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(affiliations)
    }

    private suspend fun getAffiliations(checkForVisibility: Boolean, includeUndeployd: Boolean, dfe: DataFetchingEnvironment) = if (checkForVisibility) {
        affiliationService.getAllVisibleAffiliations(dfe.token())
    } else if (includeUndeployd) {
        affiliationService.getAllAffiliationNames()
    } else {
        affiliationService.getAllDeployedAffiliations()
    }
}
