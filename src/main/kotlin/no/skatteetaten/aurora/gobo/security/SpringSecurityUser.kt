package no.skatteetaten.aurora.gobo.security

import org.springframework.security.core.userdetails.User

class SpringSecurityUser(
    username: String,
    val token: String,
    val fullName: String? = null
) : User(username, token, true, true, true, true, listOf())
