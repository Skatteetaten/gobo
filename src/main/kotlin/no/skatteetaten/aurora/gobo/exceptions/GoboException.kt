package no.skatteetaten.aurora.gobo.exceptions

open class GoboException(message: String?, cause: Throwable? = null, val code: String = "", val errorMessage: String = "") : Exception(message, cause)