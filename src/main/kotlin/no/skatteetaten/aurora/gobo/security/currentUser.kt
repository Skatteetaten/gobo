package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import graphql.servlet.GraphQLContext
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import javax.servlet.http.HttpServletRequest
import no.skatteetaten.aurora.gobo.security.User as SecurityUser

private const val UNKNOWN_USER_NAME = "Navn ukjent"
private const val GUEST_USER_ID = "anonymous"
private const val GUEST_USER_NAME = "Gjestebruker"
val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)

fun DataFetchingEnvironment.currentUser(): User {
    val request = (this.executionContext.context as GraphQLContext).httpServletRequest.get()
    return request.currentUser()
}

fun HttpServletRequest.currentUser(): User {
    val authentication = this.userPrincipal ?: return ANONYMOUS_USER
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
        User(principal.username, principal.fullName ?: UNKNOWN_USER_NAME, principal.token)
    } else {
        ANONYMOUS_USER
    }