package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import graphql.servlet.context.DefaultGraphQLServletContext
import javax.servlet.http.HttpServletRequest
import no.skatteetaten.aurora.gobo.resolvers.user.User
import no.skatteetaten.aurora.gobo.security.User as SecurityUser
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken

private const val UNKNOWN_USER_NAME = "Navn ukjent"
private const val GUEST_USER_ID = "anonymous"
private const val GUEST_USER_NAME = "Gjestebruker"
val ANONYMOUS_USER = User(GUEST_USER_ID, GUEST_USER_NAME)

fun DataFetchingEnvironment.currentUser() = User("abc", "test")

fun DataFetchingEnvironment.isAnonymousUser() = true

private fun getUser(principal: Any) =
    if (principal is SecurityUser) {
        User(principal.username, principal.fullName ?: UNKNOWN_USER_NAME, principal.token)
    } else {
        ANONYMOUS_USER
    }
