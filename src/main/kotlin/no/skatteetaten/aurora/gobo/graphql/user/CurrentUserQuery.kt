package no.skatteetaten.aurora.gobo.graphql.user

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadValue
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

data class User(val id: String, val token: String = "", val groups: List<String> = emptyList()) {
    fun name(dfe: DataFetchingEnvironment) = dfe.loadValue<String, String>(key = token, loaderClass = UserFullNameDataLoader::class)
}

@Component
class CurrentUserQuery : Query {
    @GraphQLDescription("Get current authenticated user")
    suspend fun currentUser(dfe: DataFetchingEnvironment) = dfe.currentUser()
}
