package no.skatteetaten.aurora.gobo.security

// FIXME current user test
/*
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
        assertThat(user).isEqualTo(ANONYMOUS_USER)
    }

    @Test
    fun `Get current user given principal`() {
        every { token.principal } returns User("username", "token", "fullName")
        every { request.userPrincipal } returns token

        val user = request.currentUser()
        assertThat(user.id).isEqualTo("username")
        assertThat(user.token).isEqualTo("token")
        assertThat(user.name).isEqualTo("fullName")
    }
}
*/
