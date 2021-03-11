package no.skatteetaten.aurora.gobo.integration

import no.skatteetaten.aurora.gobo.GoboException
import reactor.core.publisher.Mono

open class SourceSystemException(
    message: String,
    cause: Throwable? = null,
    code: String = "",
    errorMessage: String = message,
    val integrationResponse: String? = null,
    val sourceSystem: String? = null,
    extensions: Map<String, Any> = emptyMap()
) : GoboException(message, cause, code, errorMessage, extensions) {
    fun <T : Any> toErrorMono() = Mono.error<T>(this)
}
