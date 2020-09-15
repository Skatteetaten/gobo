package no.skatteetaten.aurora.gobo.openshift

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.security.User
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry.backoff
import java.time.Duration.ofMillis

@Component
class OpenShiftClient(@TargetService(ServiceTypes.OPENSHIFT) private val openshiftWebClient: WebClient) {
    fun user(token: String = getUserToken()): Mono<OpenShiftUser> = runCatching {
        openshiftWebClient
            .get()
            .uri("/apis/user.openshift.io/v1/users/~")
            .header(AUTHORIZATION, "Bearer $token")
            .exchange()
            .retryWhen(backoff(5, ofMillis(5000)))
            .flatMap { it.bodyToMono<OpenShiftUser>() }
    }.recoverCatching {
        throw AccessDeniedException("Unable to validate token with OpenShift!", it)
    }.getOrThrow()

    private fun getUser() = (SecurityContextHolder.getContext().authentication.principal as User)

    private fun getUserToken() = getUser().token
}
