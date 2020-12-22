package no.skatteetaten.aurora.gobo.graphql.user

import com.expediagroup.graphql.annotations.GraphQLDescription
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

data class User(val id: String, val token: String = "", val groups: List<String> = emptyList()) {
    suspend fun name(dfe: DataFetchingEnvironment) = dfe.loadOrThrow<String, UserFullName>(token).name
}

@Component
class CurrentUserQuery : Query {
    @GraphQLDescription("Get current authenticated user")
    suspend fun currentUser(dfe: DataFetchingEnvironment) = dfe.currentUser()
}
