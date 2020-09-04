package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import no.skatteetaten.aurora.gobo.resolvers.user.User
import no.skatteetaten.aurora.gobo.security.User as SecurityUser

private const val UNKNOWN_USER_NAME = "Navn ukjent"
private const val GUEST_USER_ID = "anonymous"
private const val GUEST_USER_NAME = "Gjestebruker"
val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)

suspend fun DataFetchingEnvironment.currentUser(): User =  getAuth()?.let {
    if (!it.isAuthenticated) ANONYMOUS_USER

    when (it) {
        is PreAuthenticatedAuthenticationToken -> it.principal.getUser()
        is UsernamePasswordAuthenticationToken -> it.principal.getUser()
        is AnonymousAuthenticationToken -> User(it.name, GUEST_USER_NAME)
        else -> ANONYMOUS_USER
    }
} ?: ANONYMOUS_USER

private suspend fun getAuth(): Authentication? = ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()?.authentication

private fun Any.getUser() = when {
    this is SecurityUser -> User(username, fullName ?: UNKNOWN_USER_NAME)
    else -> ANONYMOUS_USER
}

/*
fun DataFetchingEnvironment.currentUser(): User {
    val request = this.getContext<DefaultGraphQLServletContext>().httpServletRequest
    return request.currentUser()
}

fun DataFetchingEnvironment.isAnonymousUser() = this.currentUser() == ANONYMOUS_USER

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
*/
