package no.skatteetaten.aurora.gobo

open class GoboException(
    message: String,
    cause: Throwable? = null,
    val code: String? = null,
    val errorMessage: String = message,
    val extensions: Map<String, Any> = emptyMap()
) : RuntimeException(message, cause)
