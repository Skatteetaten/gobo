package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.service.AffiliationService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

@Component
class AffiliationQuery(val affiliationService: AffiliationService) : Query {

    suspend fun affiliations(
        name: String? = null, // TODO: This query variable is deprecated, replaceWith names
        names: List<String>? = null,
        checkForVisibility: Boolean? = null,
        includeUndeployed: Boolean? = null,
        dfe: DataFetchingEnvironment
    ): AffiliationsConnection {

        // TODO: This block of code below should be removed
        name?.let {
            val context = dfe.getContext<GoboGraphQLContext>()
            context?.klientid()?.let { client ->
                logger.info("Client $client is using the deprecated name query variable in the affiliation query: ${context.query}")
            }
            return AffiliationsConnection(listOf(AffiliationEdge(Affiliation(name))))
        }

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
