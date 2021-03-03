package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class MokeyIntegrationException(
    message: String,
    integrationResponse: String? = null,
    status: HttpStatus? = null
) : SourceSystemException(
    message = message,
    integrationResponse = integrationResponse,
    code = status?.reasonPhrase ?: "",
    sourceSystem = "mokey"
)
