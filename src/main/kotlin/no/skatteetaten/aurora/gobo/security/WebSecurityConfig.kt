package no.skatteetaten.aurora.gobo.security

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import javax.servlet.http.HttpServletRequest

@EnableWebSecurity
class WebSecurityConfig(
    val authenticationManager: BearerAuthenticationManager,
    @Value("\${management.server.port}") val managementPort: Int
) : WebSecurityConfigurerAdapter() {

    private val logger = LoggerFactory.getLogger(WebSecurityConfig::class.java)

    override fun configure(http: HttpSecurity) {

        http.csrf().disable()
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        http.authenticationProvider(preAuthenticationProvider())
                .addFilter(requestHeaderAuthenticationFilter())
                .authorizeRequests()
                .requestMatchers(forPort(managementPort)).permitAll()
                .antMatchers("/graphiql").permitAll()
                .antMatchers("/vendor/**").permitAll()
                .anyRequest().authenticated()
    }

    private fun forPort(port: Int) = RequestMatcher { request: HttpServletRequest -> port == request.localPort }

    @Bean
    internal fun preAuthenticationProvider() = PreAuthenticatedAuthenticationProvider().apply {
        setPreAuthenticatedUserDetailsService({ it: PreAuthenticatedAuthenticationToken ->

            val principal = it.principal as io.fabric8.openshift.api.model.User
            val fullName = principal.fullName
            val username = principal.metadata.name

            MDC.put("user", username)
            User(username, it.credentials as String, fullName).also {
                logger.info("Logged in user username=$username, name='$fullName' tokenSnippet=${it.tokenSnippet}")
            }
        })
    }

    @Bean
    internal fun requestHeaderAuthenticationFilter() = RequestHeaderAuthenticationFilter().apply {
        setPrincipalRequestHeader("Authorization")
        setExceptionIfHeaderMissing(false)
        setAuthenticationManager(authenticationManager)
    }
}