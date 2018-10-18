package no.skatteetaten.aurora.gobo.security

import assertk.assert
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import javax.servlet.http.HttpServletRequest

class UserServiceTest {

    private val request = mockk<HttpServletRequest>()
    private val token = mockk<PreAuthenticatedAuthenticationToken>(relaxed = true)

    @AfterEach
    fun tearDown() {
        clearMocks(request)
    }

    /*
    @Test
    fun `getCurrentUser() given no principal on request return anonymous`() {
        assertThat(userService.getCurrentUser().id).isEqualTo("anonymous")
    }

    @Test
    fun `Get current user given empty token return guest user`() {
        every { request.userPrincipal } returns token
        val currentUser = userService.getCurrentUser(request)
        assert(currentUser.name).isEqualTo("Gjestebruker")
    }
    */
}