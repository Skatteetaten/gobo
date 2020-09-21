package no.skatteetaten.aurora.gobo.security

import kotlin.math.min
import org.springframework.security.core.userdetails.User

class SpringSecurityUser(
    username: String,
    val token: String,
    val fullName: String? = null
) : User(username, token, true, true, true, true, listOf()) {

    val tokenSnippet: String
        get() = token.substring(0, min(token.length, 5))
}
