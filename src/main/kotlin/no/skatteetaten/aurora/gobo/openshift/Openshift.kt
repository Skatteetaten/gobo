package no.skatteetaten.aurora.gobo.openshift

import reactor.core.publisher.Mono

interface Openshift {
    fun user(token: String): Mono<OpenshiftUser>
}
