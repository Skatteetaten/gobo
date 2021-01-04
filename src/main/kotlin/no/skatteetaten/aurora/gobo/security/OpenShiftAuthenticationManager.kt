package no.skatteetaten.aurora.gobo.security

import com.fkorotkov.kubernetes.authentication.newTokenReview
import com.fkorotkov.kubernetes.authentication.spec
import mu.KotlinLogging
import no.skatteetaten.aurora.kubernetes.ClientTypes
import no.skatteetaten.aurora.kubernetes.KubernetesReactorClient
import no.skatteetaten.aurora.kubernetes.TargetClient
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.empty
import reactor.core.publisher.Mono.just

private val logger = KotlinLogging.logger {}

@Component
class OpenShiftAuthenticationManager(@TargetClient(ClientTypes.SERVICE_ACCOUNT) private val kubernetesClient: KubernetesReactorClient) :
    ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = runCatching {
        currentUser(authentication.credentials.toString())
    }.mapCatching { monoUser ->
        monoUser.flatMap {
            just(
                PreAuthenticatedAuthenticationToken(
                    it,
                    authentication.credentials.toString(),
                    it.status.user.groups
                        .apply { it.status.user?.username?.let { username -> add(username) } }
                        .filter { group: String? -> group?.isNotEmpty() ?: false }
                        .map { authority -> SimpleGrantedAuthority(authority) }
                ) as Authentication
            )
        }
    }.onFailure {
        logger.warn(it) { "Failed authentication!" }
    }.getOrElse { empty() }

    private fun currentUser(token: String) = runCatching {
        val tokenReview = newTokenReview {
            spec {
                this.token = token
            }
        }
        kubernetesClient.post(tokenReview).doOnError {
            if (it is WebClientResponseException && it.statusCode == HttpStatus.FORBIDDEN) {
                throw AccessDeniedException("Invalid OpenShift token", it)
            }
        }
    }.recoverCatching {
        throw AccessDeniedException("Unable to validate token with OpenShift", it)
    }.getOrThrow()
}
