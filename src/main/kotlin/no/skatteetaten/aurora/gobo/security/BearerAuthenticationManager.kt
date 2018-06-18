package no.skatteetaten.aurora.gobo.security

import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.openshift.client.DefaultOpenShiftClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class BearerAuthenticationManager : AuthenticationManager {

    val logger: Logger = LoggerFactory.getLogger(BearerAuthenticationManager::class.java)

    companion object {
        private val headerPattern: Pattern = Pattern.compile("Bearer\\s+(.*)", Pattern.CASE_INSENSITIVE)

        private fun getBearerTokenFromAuthentication(authentication: Authentication?): String {
            val authenticationHeaderValue = authentication?.principal?.toString()
            val matcher = headerPattern.matcher(authenticationHeaderValue)
            if (!matcher.find()) {
                throw BadCredentialsException("Unexpected Authorization header format")
            }
            return matcher.group(1)
        }
    }

    override fun authenticate(authentication: Authentication?): Authentication {

        val token = getBearerTokenFromAuthentication(authentication)
        val client = DefaultOpenShiftClient(ConfigBuilder().withOauthToken(token).build())
        val openShiftUser = client.currentUser()
        return PreAuthenticatedAuthenticationToken(openShiftUser, token)
    }
}
