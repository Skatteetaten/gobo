package no.skatteetaten.aurora.gobo.integration

open class GoboException(message: String, cause: Throwable? = null, val code: String = "", val errorMessage: String = message) : RuntimeException(message, cause)

class SourceSystemException(message: String, cause: Throwable? = null, code: String = "", errorMessage: String = message) : GoboException(message, cause, code, errorMessage)