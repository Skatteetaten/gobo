package no.skatteetaten.aurora.gobo.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import reactor.core.publisher.Mono

class CurrentUserTest {
    private val token = mockk<PreAuthenticatedAuthenticationToken>(relaxed = true)
    private val securityContext = mockk<SecurityContext> {
        every { authentication } returns token
    }
    private val dfe = mockk<DataFetchingEnvironment>()

    @BeforeEach
    fun setUp() {
        every { dfe.graphQlContext } returns GraphQLContext.newContext().putAll(mapOf("token" to "token", "securityContext" to Mono.just(securityContext))).build()
    }

    @AfterEach
    fun tearDown() {
        clearMocks(dfe, securityContext, token)
    }

    @Test
    fun `Get current user given no principal return anonymous user`() {
        val user = runBlocking { dfe.currentUser() }
        assertThat(user).isEqualTo(ANONYMOUS_USER)
    }

    @Test
    fun `Get current user given principal`() {
        every { token.principal } returns SpringSecurityUser("username", "token", "fullName")
        every { token.credentials } returns "token"
        every { securityContext.authentication } returns token

        val user = runBlocking { dfe.currentUser() }
        assertThat(user.id).isEqualTo("username")
        assertThat(user.token).isEqualTo("token")
    }
}
