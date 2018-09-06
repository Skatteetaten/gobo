package no.skatteetaten.aurora.gobo.resolvers.exceptions

class ResolverException(message: String?, cause: Throwable? = null, val code: String = "", val errorMessage: String = "") : RuntimeException(message, cause)