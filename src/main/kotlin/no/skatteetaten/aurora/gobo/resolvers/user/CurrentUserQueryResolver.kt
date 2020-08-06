package no.skatteetaten.aurora.gobo.resolvers.user

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class CurrentUserQueryResolver : Query {
    fun getCurrentUser(dfe: DataFetchingEnvironment) = dfe.currentUser()
}
