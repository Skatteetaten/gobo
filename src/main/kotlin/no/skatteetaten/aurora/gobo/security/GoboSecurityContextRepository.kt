package no.skatteetaten.aurora.gobo.security

import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.empty
import java.security.Principal

@Component
class GoboSecurityContextRepository(
        private val authenticationManager: OpenShiftAuthenticationManager
) : ServerSecurityContextRepository {
    override fun save(
            exchange: ServerWebExchange,
            context: SecurityContext
    ): Mono<Void> = throw UnsupportedOperationException("Not supported yet!")

    override fun load(exchange: ServerWebExchange): Mono<SecurityContext> {
        val request = exchange.request
        val authHeader = request.headers.getFirst(AUTHORIZATION)

        return when {
            authHeader != null && authHeader.startsWith("Bearer ") -> {
                val authToken = authHeader.substring(7)
                val auth = UsernamePasswordAuthenticationToken(authToken, authToken)

                this.authenticationManager.authenticate(auth).map { SecurityContextImpl(it) }
            }
            else -> empty() // return just med SecurityContext med anonymous user
        }
    }
}