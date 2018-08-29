package no.skatteetaten.aurora.gobo.security

import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.openshift.client.DefaultOpenShiftClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component

@Component
class OpenShiftAuthenticationUserDetailsService : AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private val logger = LoggerFactory.getLogger(OpenShiftAuthenticationUserDetailsService::class.java)

    override fun loadUserDetails(token: PreAuthenticatedAuthenticationToken): UserDetails {
        val principal = token.principal as io.fabric8.openshift.api.model.User

        return toUserDetails(principal, token.credentials as String).also { user ->
            MDC.put("user", user.username)
            logger.info("Current user username=${user.username}, name='${user.fullName}' tokenSnippet=${user.tokenSnippet}")
        }
    }

    fun toUserDetails(principal: io.fabric8.openshift.api.model.User, userToken: String): User {
        val fullName = principal.fullName
        val username = principal.metadata.name
        return User(username, userToken, fullName)
    }
}

@Component
class OpenShiftUserLoader {
    fun findOpenShiftUserByToken(token: String): io.fabric8.openshift.api.model.User? =
            DefaultOpenShiftClient(ConfigBuilder().withOauthToken(token).build()).currentUser()
}
