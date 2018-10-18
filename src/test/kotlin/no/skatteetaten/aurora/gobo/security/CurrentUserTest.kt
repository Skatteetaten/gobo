package no.skatteetaten.aurora.gobo.security

import assertk.assert
import assertk.assertions.isEqualTo
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import javax.servlet.http.HttpServletRequest

class CurrentUserTest {

    private val request = mockk<HttpServletRequest>(relaxed = true)
    private val token = mockk<PreAuthenticatedAuthenticationToken>()

    @AfterEach
    fun tearDown() {
        clearMocks(request, token)
    }

    @Test
    fun `Get current user given no principal return anonymous user`() {
        val user = request.currentUser()
        assert(user).isEqualTo(ANONYMOUS_USER)
    }

    @Test
    fun `Get current user given principal`() {
        every { token.principal } returns User("username", "token", "fullName")
        every { request.userPrincipal } returns token

        val user = request.currentUser()
        assert(user.id).isEqualTo("username")
        assert(user.token).isEqualTo("token")
        assert(user.name).isEqualTo("fullName")
    }
}