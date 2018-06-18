package no.skatteetaten.aurora.gobo.user

import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class UserService {
    fun getCurrentUser(): User {

        val principal = SecurityContextHolder.getContext().authentication.principal
        val user = principal as no.skatteetaten.aurora.gobo.security.User

        return User(user.username, user.fullName ?: "Ukjent bruker")
    }
}