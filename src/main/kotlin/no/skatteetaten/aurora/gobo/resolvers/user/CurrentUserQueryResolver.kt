package no.skatteetaten.aurora.gobo.resolvers.user

import com.expediagroup.graphql.spring.operations.Query
import org.springframework.stereotype.Component
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.reactive.awaitFirst
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.affiliation.AffiliationEdge
import no.skatteetaten.aurora.gobo.resolvers.affiliation.AffiliationsConnection
import no.skatteetaten.aurora.gobo.resolvers.token
import no.skatteetaten.aurora.gobo.security.currentUser

@Component
class CurrentUserQuery : Query {
    fun getCurrentUser(dfe: DataFetchingEnvironment) = dfe.currentUser()
}

@Component
class AffiliationQuery(val affiliationService: AffiliationService) : Query {

    suspend fun affiliations(
            name: String?,
            checkForVisibility: Boolean?,
            dfe: DataFetchingEnvironment
    ): AffiliationsConnection {

        val affiliationNames = if (name == null) {
            getAffiliations(checkForVisibility ?: false, dfe.token())
        } else {
            listOf(name)
        }

        val affiliations = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(affiliations)
    }

    private suspend fun getAffiliations(checkForVisibility: Boolean, token: String) = if (checkForVisibility) {
        affiliationService.getAllVisibleAffiliations(token).awaitFirst()
    } else {
        affiliationService.getAllAffiliations().awaitFirst()
    }
}