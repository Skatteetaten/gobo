package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.removeNewLines
import org.springframework.http.HttpStatus

class MokeyIntegrationException(
    message: String,
    integrationResponse: String? = null,
    status: HttpStatus? = null
) : SourceSystemException(
    message = message,
    integrationResponse = integrationResponse?.removeNewLines(),
    code = status?.reasonPhrase ?: "",
    sourceSystem = "mokey"
)
