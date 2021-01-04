package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import io.fabric8.kubernetes.api.model.authentication.TokenReview
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.AccessDeniedException
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.graphql.user.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken

const val UNKNOWN_USER_NAME = "Navn ukjent"
const val GUEST_USER_ID = "anonymous"
const val GUEST_USER_NAME = "Gjestebruker"
val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)

fun DataFetchingEnvironment.checkValidUserToken() {
    token()
    if (currentUser() == ANONYMOUS_USER) {
        throw AccessDeniedException("Valid authentication token required")
    }
}

fun DataFetchingEnvironment.currentUser() = this.getContext<GoboGraphQLContext>().securityContext?.authentication?.let {
    if (!it.isAuthenticated) ANONYMOUS_USER
    when (it) {
        is PreAuthenticatedAuthenticationToken, is UsernamePasswordAuthenticationToken -> it.principal.getUser(it.credentials.toString())
        is AnonymousAuthenticationToken -> User(it.name, GUEST_USER_NAME)
        else -> ANONYMOUS_USER
    }
} ?: ANONYMOUS_USER

private val logger = KotlinLogging.logger {}

private fun Any.getUser(token: String) = when (this) {
    is io.fabric8.openshift.api.model.User -> User(id = metadata.name, token = token)
    is TokenReview -> User(
        id = status?.user?.username ?: UNKNOWN_USER_NAME.also { logger.warn("Unknown user: $status") },
        token = token,
        groups = status.user.groups
    )
    is SpringSecurityUser -> User(id = username, token = token)
    is org.springframework.security.core.userdetails.User -> User(id = username, token = token)
    else -> ANONYMOUS_USER
}
