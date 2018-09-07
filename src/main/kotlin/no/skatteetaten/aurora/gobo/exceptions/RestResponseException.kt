package no.skatteetaten.aurora.gobo.exceptions

class RestResponseException(message: String?, cause: Throwable? = null, code: String = "", errorMessage: String = "") : GoboException(message, cause, code, errorMessage)