package no.skatteetaten.aurora.gobo.security

import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class WebSecurityConfig(
    private val authenticationManager: OpenShiftAuthenticationManager,
    private val securityContextRepository: GoboSecurityContextRepository
) {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http
        .httpBasic().disable()
        .formLogin().disable()
        .csrf().disable()
        .logout().disable()
        .authenticationManager(authenticationManager)
        .securityContextRepository(securityContextRepository)
        .authorizeExchange().pathMatchers("/**").permitAll()
        .anyExchange().authenticated()
        .and()
        .build()
}
