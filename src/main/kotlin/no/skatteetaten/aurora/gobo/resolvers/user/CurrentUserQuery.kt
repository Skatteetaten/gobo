package no.skatteetaten.aurora.gobo.resolvers.user

import com.expediagroup.graphql.annotations.GraphQLDescription
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class CurrentUserQuery : Query {
    @GraphQLDescription("Get current authenticated user")
    suspend fun currentUser(dfe: DataFetchingEnvironment) = dfe.currentUser()
}
