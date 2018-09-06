package no.skatteetaten.aurora.gobo.security

import no.skatteetaten.aurora.gobo.security.UserService.Companion.GUEST_USER_ID
import no.skatteetaten.aurora.gobo.security.UserService.Companion.GUEST_USER_NAME
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@Component
class MockUserDetailsService : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails {
        return User("aurora", "token", "Aurora OpenShift Test User")
    }
}

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [UserService::class, MockUserDetailsService::class])
class UserServiceTest {

    @Autowired
    lateinit var userService: UserService

    @Test
    @WithUserDetails("aurora")
    fun `getCurrentUser() when logged in`() {

        assertThat(userService.getCurrentUser().id).isEqualTo("aurora")
    }

    @Test
    @WithAnonymousUser
    fun `getCurrentUser() when not logged in returns guest user`() {

        userService.getCurrentUser().apply {
            assertThat(id).isEqualTo(GUEST_USER_ID)
            assertThat(name).isEqualTo(GUEST_USER_NAME)
        }
    }
}