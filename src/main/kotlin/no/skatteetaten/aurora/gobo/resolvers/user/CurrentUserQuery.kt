package no.skatteetaten.aurora.gobo.resolvers.user

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class CurrentUserQuery : Query {
    suspend fun currentUser(dfe: DataFetchingEnvironment) = dfe.currentUser()
}

// @Component
// class AffiliationQuery(val affiliationService: AffiliationService) : Query {
//
//    suspend fun affiliations(
//        name: String?,
//        checkForVisibility: Boolean?,
//        dfe: DataFetchingEnvironment
//    ): AffiliationsConnection {
//
//        val affiliationNames = if (name == null) {
//            getAffiliations(checkForVisibility ?: false, dfe.token())
//        } else {
//            listOf(name)
//        }
//
//        val affiliations = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
//        return AffiliationsConnection(affiliations)
//    }
//
//    private suspend fun getAffiliations(checkForVisibility: Boolean, token: String) = if (checkForVisibility) {
//        affiliationService.getAllVisibleAffiliations(token).awaitFirst()
//    } else {
//        affiliationService.getAllAffiliations().awaitFirst()
//    }
// }
