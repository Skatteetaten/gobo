package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import io.fabric8.kubernetes.api.model.authentication.TokenReview
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.graphql.user.User
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken

const val UNKNOWN_USER_NAME = "Navn ukjent"
const val GUEST_USER_ID = "anonymous"
const val GUEST_USER_NAME = "Gjestebruker"
val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)

suspend fun DataFetchingEnvironment.checkValidUserToken() {
    getValidUser()
}

suspend fun DataFetchingEnvironment.checkIsUserAuthorized(allowedAdGroup: String) {
    val user = this.getValidUser()

    if (!user.groups.contains(allowedAdGroup)) {
        throw AccessDeniedException("You do not have access to perform this operation")
    }
}

suspend fun <T> DataFetchingEnvironment.ifValidUserToken(whenValidToken: suspend () -> T): T {
    getValidUser()
    return whenValidToken()
}

suspend fun DataFetchingEnvironment.getValidUser(): User {
    token()
    return currentUser().also {
        if (it == ANONYMOUS_USER) throw AccessDeniedException("Valid authentication token required")
    }
}

suspend fun DataFetchingEnvironment.currentUser() =
    this.getContext<GoboGraphQLContext>().securityContext().authentication?.let {
        if (!it.isAuthenticated) ANONYMOUS_USER
        when (it) {
            is PreAuthenticatedAuthenticationToken, is UsernamePasswordAuthenticationToken -> it.principal.getUser(it.credentials.toString())
            is AnonymousAuthenticationToken -> User(it.name, GUEST_USER_NAME)
            else -> ANONYMOUS_USER
        }
    } ?: ANONYMOUS_USER

private val logger = KotlinLogging.logger {}

private fun Any.getUser(token: String) = when (this) {
    is io.fabric8.openshift.api.model.User -> User(id = metadata.name, token = token, groups = groups)
    is TokenReview -> User(
        id = status?.user?.username ?: UNKNOWN_USER_NAME.also { logger.warn("Unknown user: $status") },
        token = token,
        groups = status?.user?.groups ?: emptyList()
    )
    is SpringSecurityUser -> User(id = username, token = token, groups = authorities.map { it.authority })
    is org.springframework.security.core.userdetails.User -> User(
        id = username,
        token = token,
        groups = authorities.map { it.authority }
    )
    else -> ANONYMOUS_USER
}
