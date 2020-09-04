package no.skatteetaten.aurora.gobo.openshift

import reactor.core.publisher.Mono

interface OpenShift {
    fun user(token: String): Mono<OpenshiftUser>
}
