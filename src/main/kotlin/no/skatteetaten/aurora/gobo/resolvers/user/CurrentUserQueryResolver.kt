package no.skatteetaten.aurora.gobo.resolvers.user

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class CurrentUserQueryResolver : GraphQLQueryResolver {
    fun getCurrentUser(dfe: DataFetchingEnvironment) = dfe.currentUser()
}