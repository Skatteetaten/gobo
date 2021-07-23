package no.skatteetaten.aurora.gobo.graphql

import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.boober.RedeployResponse

class IntegrationDisabledException(message: String) : GoboException(message = message)

class ApplicationRedeployException(message: String, cause: Throwable? = null, code: String, redeployResponse: RedeployResponse) :
    GoboException(message = message, cause = cause, errorMessage = message, code = code, extensions = mapOf("applicationDeployment" to redeployResponse))
