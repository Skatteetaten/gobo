package no.skatteetaten.aurora.gobo.resolvers

import no.skatteetaten.aurora.gobo.GoboException

class AccessDeniedException(message: String, cause: Throwable? = null, errorMessage: String = message) :
    GoboException(message = message, cause = cause, errorMessage = errorMessage)

class MissingLabelException(message: String, cause: Throwable? = null, errorMessage: String = message) :
    GoboException(message = message, cause = cause, errorMessage = errorMessage)

class IntegrationDisabledException(message: String) : GoboException(message = message)
