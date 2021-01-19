package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.spring.exception.SimpleKotlinGraphQLError
import com.expediagroup.graphql.spring.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

@Component
class AffiliationQuery(val affiliationService: AffiliationService) : Query {

    suspend fun affiliations(
        name: String?, // TODO: This query variable is deprecated, replaceWith names
        names: List<String>?,
        checkForVisibility: Boolean?,
        dfe: DataFetchingEnvironment
    ): DataFetcherResult<AffiliationsConnection> {

        // TODO: This block of code below should be removed
        name?.let {
            val context = dfe.getContext<GoboGraphQLContext>()
            context?.klientid()?.let { client ->
                logger.info("Client $client is using the deprecated name query variable in the affiliation query: ${context.query}")
            }

            val affiliations = listOf(AffiliationEdge(Affiliation(it)))
            return DataFetcherResult.newResult<AffiliationsConnection>().data(AffiliationsConnection(affiliations)).error(
                SimpleKotlinGraphQLError(IllegalArgumentException("This query variable is deprecated, replaceWith names"))
            ).build()
        }

        val affiliationNames = if (names.isNullOrEmpty())
            getAffiliations(checkForVisibility ?: false, dfe)
        else
            names

        val affiliations = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return DataFetcherResult.newResult<AffiliationsConnection>().data(AffiliationsConnection(affiliations)).build()
    }

    private suspend fun getAffiliations(checkForVisibility: Boolean, dfe: DataFetchingEnvironment) =
        if (checkForVisibility) {
            affiliationService.getAllVisibleAffiliations(dfe.token())
        } else {
            affiliationService.getAllAffiliations()
        }
}
