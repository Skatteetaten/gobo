package no.skatteetaten.aurora.gobo.security

import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class OpenShiftAuthenticationUserDetailsService :
    AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    override fun loadUserDetails(token: PreAuthenticatedAuthenticationToken): UserDetails {
        val user = token.principal as User
        MDC.put("user", user.username)
        return user
    }
}

@Component
class OpenShiftUserLoader {
    fun findOpenShiftUserByToken(token: String): User {
        return User("testuser", token, "Test Testesen") // TODO fetch user from tokenReview
    }
}
