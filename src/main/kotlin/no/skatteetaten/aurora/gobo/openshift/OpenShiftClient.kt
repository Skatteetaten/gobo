package no.skatteetaten.aurora.gobo.openshift

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.security.User
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry.backoff
import java.time.Duration.ofMillis

private val logger = KotlinLogging.logger {}

@Component
class OpenShiftClient(private val openshiftWebClient: WebClient) {
    fun user(token: String = getUserToken()): Mono<OpenShiftUser> = runCatching {
        // TODO: Remove!
        logger.debug { "Incoming token: $token" }

        openshiftWebClient
            .get()
            .uri("/apis/user.openshift.io/v1/users/~")
            .header(AUTHORIZATION, "Bearer $token")
            .exchange()
            .retryWhen(backoff(5, ofMillis(5000)))
            .flatMap { it.bodyToMono(OpenShiftUser::class.java) }
    }.recoverCatching {
        throw AccessDeniedException("Unable to validate token with OpenShift!", it)
    }.getOrThrow()

    private fun getUser() = (SecurityContextHolder.getContext().authentication.principal as User)

    private fun getUserToken() = getUser().token
}
