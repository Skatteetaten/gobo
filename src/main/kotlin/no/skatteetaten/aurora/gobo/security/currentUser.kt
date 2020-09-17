package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.openshift.OpenShiftUser
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import no.skatteetaten.aurora.gobo.security.User as SecurityUser

private const val UNKNOWN_USER_NAME = "Navn ukjent"
private const val GUEST_USER_ID = "anonymous"
private const val GUEST_USER_NAME = "Gjestebruker"
val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)

fun DataFetchingEnvironment.currentUser(): User {

    val authentication = this.getContext<GoboGraphQLContext>().securityContext?.authentication

    return authentication?.let {
        if (!it.isAuthenticated) ANONYMOUS_USER
        when (it) {
            is PreAuthenticatedAuthenticationToken -> it.principal.getUser(it.credentials.toString())
            is UsernamePasswordAuthenticationToken -> it.principal.getUser(it.credentials.toString())
            is AnonymousAuthenticationToken -> User(it.name, GUEST_USER_NAME)
            else -> ANONYMOUS_USER
        }
    } ?: ANONYMOUS_USER
}

private fun Any.getUser() = this.getUser("")

private fun Any.getUser(token: String) = when {
    this is OpenShiftUser -> User(metadata.name, fullName, token)
    this is SecurityUser -> User(username, fullName ?: UNKNOWN_USER_NAME, token)
    else -> ANONYMOUS_USER
}
