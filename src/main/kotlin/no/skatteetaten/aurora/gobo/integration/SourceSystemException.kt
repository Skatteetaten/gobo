package no.skatteetaten.aurora.gobo.integration

import no.skatteetaten.aurora.gobo.GoboException

class SourceSystemException(
    message: String,
    cause: Throwable? = null,
    code: String = "",
    errorMessage: String = message,
    val sourceSystem: String? = null
) : GoboException(message, cause, code, errorMessage)
