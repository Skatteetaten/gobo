package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.BooberAffiliationService
import no.skatteetaten.aurora.gobo.integration.mokey.MokeyAffiliationService
import org.springframework.stereotype.Component

@Component
class AffiliationQuery(val mokeyAffiliationService: MokeyAffiliationService, val booberAffiliationService: BooberAffiliationService) : Query {

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
        mokeyAffiliationService.getAllVisibleAffiliations(dfe.token())
    } else if (includeUndeployd) {
        booberAffiliationService.getAllAffiliationNames()
    } else {
        mokeyAffiliationService.getAllDeployedAffiliations()
    }
}
