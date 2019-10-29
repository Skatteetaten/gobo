package no.skatteetaten.aurora.gobo.security

import javax.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher

@EnableWebSecurity
class WebSecurityConfig(
    val authenticationManager: BearerAuthenticationManager,
    val openShiftAuthenticationUserDetailsService: OpenShiftAuthenticationUserDetailsService,
    @Value("\${management.server.port}") val managementPort: Int
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {

        http.csrf().disable()
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        http.authenticationProvider(preAuthenticationProvider(openShiftAuthenticationUserDetailsService))
            .addFilter(requestHeaderAuthenticationFilter())
            .authorizeRequests()
            .requestMatchers(forPort(managementPort)).permitAll()
            .antMatchers("/public/**").permitAll()
            .antMatchers("/graphql").permitAll()
            .antMatchers("/altair").permitAll()
            .antMatchers("/voyager").permitAll()
            .antMatchers("/vendor/**").permitAll()
            .anyRequest().authenticated()
    }

    private fun forPort(port: Int) = RequestMatcher { request: HttpServletRequest -> port == request.localPort }

    @Bean
    internal fun preAuthenticationProvider(openShiftAuthenticationUserDetailsService: OpenShiftAuthenticationUserDetailsService) =
        PreAuthenticatedAuthenticationProvider()
            .apply { setPreAuthenticatedUserDetailsService(openShiftAuthenticationUserDetailsService) }

    @Bean
    internal fun requestHeaderAuthenticationFilter() = RequestHeaderAuthenticationFilter().apply {
        setPrincipalRequestHeader("Authorization")
        setExceptionIfHeaderMissing(false)
        setAuthenticationManager(authenticationManager)
    }
}
