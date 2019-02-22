package no.skatteetaten.aurora.gobo

open class GoboException(
    message: String,
    cause: Throwable? = null,
    val code: String? = null,
    val errorMessage: String = message
) : RuntimeException(message, cause)