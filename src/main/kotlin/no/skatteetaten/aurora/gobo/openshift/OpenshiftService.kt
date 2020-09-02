package no.skatteetaten.aurora.gobo.openshift

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@Profile("!mockOpenShift")
class OpenShiftService(private val openshiftClient: OpenshiftClient) : Openshift {
    override fun user(token: String): Mono<OpenshiftUser> = openshiftClient.user(token)
}