package no.skatteetaten.aurora.gobo.openshift

import no.skatteetaten.aurora.gobo.security.User
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry.backoff
import java.time.Duration.ofMillis

@Component
class OpenShiftClient(private val openshiftWebClient: WebClient) {
    fun user(token: String = getUserToken()): Mono<OpenshiftUser> = runCatching {
        openshiftWebClient
            .get()
            .uri("/apis/user.openshift.io/v1/users/~")
            .header(AUTHORIZATION, "Bearer $token")
            .exchange()
            .retryWhen(backoff(5, ofMillis(5000)))
            .flatMap { it.bodyToMono(OpenshiftUser::class.java) }
    }.recoverCatching {
        throw AccessDeniedException("Unable to validate token with OpenShift!", it)
    }.getOrThrow()

    fun getUser() = (SecurityContextHolder.getContext().authentication.principal as User)

    private fun getUserToken() = (SecurityContextHolder.getContext().authentication.principal as User).token
}
