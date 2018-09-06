package no.skatteetaten.aurora.gobo.resolvers.user

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.security.UserService
import org.springframework.stereotype.Component

@Component
class CurrentUserQueryResolver(
    val userService: UserService
) : GraphQLQueryResolver {

    fun getCurrentUser(): User {

        return userService.getCurrentUser()
    }
}