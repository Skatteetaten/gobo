package no.skatteetaten.aurora.gobo.user

import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class UserService {
    companion object {
        const val UNKNOWN_USER_NAME = "Navn ukjent"
        const val GUEST_USER_ID = "anonymous"
        const val GUEST_USER_NAME = "Gjestebruker"
    }


    fun getCurrentUser(): User {

        val context = SecurityContextHolder.getContext()
        val authentication: Authentication? = context.authentication

        return if (authentication != null) {
            val principal = authentication.principal
            val user = principal as no.skatteetaten.aurora.gobo.security.User
            User(user.username, user.fullName ?: UNKNOWN_USER_NAME)
        } else {
            User(GUEST_USER_ID, GUEST_USER_NAME)
        }
    }
}