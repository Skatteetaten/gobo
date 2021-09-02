package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.service.AffiliationService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

@Component
class AffiliationQuery(val affiliationService: AffiliationService) : Query {

    suspend fun affiliations(
        names: List<String>? = null,
        checkForVisibility: Boolean? = null,
        includeUndeployed: Boolean? = null,
        dfe: DataFetchingEnvironment
    ): AffiliationsConnection {

        val affiliationNames = if (names.isNullOrEmpty()) {
            getAffiliations(checkForVisibility ?: false, includeUndeployed ?: false, dfe)
        } else {
            names
        }

        val affiliations = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(affiliations)
    }

    private suspend fun getAffiliations(
        checkForVisibility: Boolean,
        includeUndeployed: Boolean,
        dfe: DataFetchingEnvironment
    ) =
        when {
            checkForVisibility -> affiliationService.getAllVisibleAffiliations(dfe.token())
            includeUndeployed -> affiliationService.getAllAffiliationNames()
            else -> affiliationService.getAllDeployedAffiliations()
        }
}
