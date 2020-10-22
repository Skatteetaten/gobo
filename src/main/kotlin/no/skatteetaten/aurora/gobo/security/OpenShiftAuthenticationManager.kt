package no.skatteetaten.aurora.gobo.security

import mu.KotlinLogging
import no.skatteetaten.aurora.kubernetes.KubernetesReactorClient
import org.springframework.security.access.AccessDeniedException
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
class OpenShiftAuthenticationManager(private val kubernetesClient: KubernetesReactorClient) :
    ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> = runCatching {
        currentUser(authentication.credentials.toString())
    }.mapCatching { monoUser ->
        monoUser.flatMap {
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

    private fun currentUser(token: String) = runCatching {
        kubernetesClient.currentUser(token)
    }.recoverCatching {
        throw AccessDeniedException("Unable to validate token with OpenShift!", it)
    }.getOrThrow()
}
