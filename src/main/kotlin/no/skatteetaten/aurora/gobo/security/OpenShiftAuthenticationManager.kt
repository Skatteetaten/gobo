package no.skatteetaten.aurora.gobo.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.openshift.OpenShift
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.empty
import reactor.core.publisher.Mono.just

private val logger = KotlinLogging.logger {}

@Component
class OpenShiftAuthenticationManager(private val openShiftService: OpenShift) : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> = runCatching {
        openShiftService.user(authentication.credentials.toString())
    }.mapCatching { monoUser ->
        monoUser.flatMap {
            logger.debug { "Received user: ${jacksonObjectMapper().writeValueAsString(it)}" }

            just(
                PreAuthenticatedAuthenticationToken(
                    it,
                    authentication.credentials.toString(),
                    listOf(it.identities, it.groups)
                        .flatten()
                        .map { authority -> SimpleGrantedAuthority(authority) }
                ) as Authentication
            )
        }
    }.onFailure {
        logger.warn(it) { "Failed authentication!" }
    }.getOrElse { empty() }
}
