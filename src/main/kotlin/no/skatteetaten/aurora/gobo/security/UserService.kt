package no.skatteetaten.aurora.gobo.security

import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import no.skatteetaten.aurora.gobo.security.User as SecurityUser

@Component
class UserService {
    companion object {
        const val UNKNOWN_USER_NAME = "Navn ukjent"
        const val GUEST_USER_ID = "anonymous"
        const val GUEST_USER_NAME = "Gjestebruker"
        val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)
    }

    fun getCurrentUser(request: HttpServletRequest? = null): User {
        val authentication = request?.userPrincipal ?: return ANONYMOUS_USER
        return when (authentication) {
            is PreAuthenticatedAuthenticationToken -> {
                getUser(authentication.principal)
            }
            is UsernamePasswordAuthenticationToken -> {
                getUser(authentication.principal)
            }
            is AnonymousAuthenticationToken -> User(authentication.name, GUEST_USER_NAME)
            else -> ANONYMOUS_USER
        }
    }

    private fun getUser(principal: Any) =
        if (principal is SecurityUser) {
            User(principal.username, principal.fullName ?: UNKNOWN_USER_NAME)
        } else {
            ANONYMOUS_USER
        }

    fun getToken(): String {
        val principal = SecurityContextHolder.getContext()?.authentication?.principal ?: return ""
        return when (principal) {
            is SecurityUser -> principal.token
            else -> ""
        }
    }
}