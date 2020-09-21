package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import io.fabric8.openshift.api.model.User as KubernetesUser

private const val UNKNOWN_USER_NAME = "Navn ukjent"
private const val GUEST_USER_ID = "anonymous"
private const val GUEST_USER_NAME = "Gjestebruker"
val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)

fun DataFetchingEnvironment.currentUser() = this.getContext<GoboGraphQLContext>().securityContext?.authentication?.let {
    if (!it.isAuthenticated) ANONYMOUS_USER
    when (it) {
        is PreAuthenticatedAuthenticationToken, is UsernamePasswordAuthenticationToken -> it.principal.getUser(it.credentials.toString())
        is AnonymousAuthenticationToken -> User(it.name, GUEST_USER_NAME)
        else -> ANONYMOUS_USER
    }
} ?: ANONYMOUS_USER

private fun Any.getUser(token: String) = when (this) {
    is KubernetesUser -> User(metadata.name, fullName, token)
    is SpringSecurityUser -> User(username, fullName ?: UNKNOWN_USER_NAME, token)
    else -> ANONYMOUS_USER
}
