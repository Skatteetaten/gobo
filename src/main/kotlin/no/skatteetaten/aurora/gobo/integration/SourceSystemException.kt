package no.skatteetaten.aurora.gobo.integration

import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.ServiceTypes
import reactor.core.publisher.Mono

open class SourceSystemException(
    message: String,
    cause: Throwable? = null,
    code: String = "",
    errorMessage: String = message,
    val integrationResponse: String? = null,
    val sourceSystem: ServiceTypes? = null,
    extensions: Map<String, Any> = emptyMap()
) : GoboException(message, cause, code, errorMessage, extensions) {
    fun <T : Any> toErrorMono(): Mono<T> = Mono.error(this)
}
