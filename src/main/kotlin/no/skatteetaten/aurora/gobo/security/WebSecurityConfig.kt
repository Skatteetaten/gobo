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
//    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http
//        .exceptionHandling()
//        .authenticationEntryPoint { exchange, _ -> Mono.fromRunnable { exchange.response.statusCode = HttpStatus.UNAUTHORIZED } }
//        .accessDeniedHandler { exchange, _ -> Mono.fromRunnable { exchange.response.statusCode = HttpStatus.FORBIDDEN } }
//        .and()
//        .csrf().disable()
//        .formLogin().disable()
//        .httpBasic().disable()
//        .authenticationManager(authenticationManager)
//        .securityContextRepository(securityContextRepository)
//        .authorizeExchange()
//        .pathMatchers(
//            "/docs/v3/api-docs",
//            "/docs/v3/api-docs.yaml",
//            "/docs/v3/api-docs/swagger-config",
//            "/docs/swagger-ui.html",
//            "/docs/webjars/swagger-ui/**",
//            "/actuator",
//            "/actuator/**"
//        ).permitAll()
//        .pathMatchers(HttpMethod.OPTIONS).permitAll()
//        .anyExchange().authenticated()
//        .and()
//        .build()

// import org.springframework.context.annotation.Bean
// import org.springframework.context.annotation.Configuration
// import org.springframework.security.config.web.server.ServerHttpSecurity
// import org.springframework.security.web.server.SecurityWebFilterChain
//
// @Configuration
// class WebSecurityConfig {
//
// @Bean
// fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {
// http.httpBasic().disable()
// http.formLogin().disable()
// http.csrf().disable()
// http.logout().disable()
//
// http.authorizeExchange().pathMatchers("/**").permitAll()
// http.authorizeExchange().anyExchange().authenticated()
//
// return http.build()
// }
// }
//
