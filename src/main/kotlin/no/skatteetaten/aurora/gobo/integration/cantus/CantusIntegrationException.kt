package no.skatteetaten.aurora.gobo.integration.cantus

import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class CantusIntegrationException(
    message: String,
    integrationResponse: String,
    status: HttpStatus? = null
) :
    SourceSystemException(
        message = message,
        code = status?.reasonPhrase ?: "",
        integrationResponse = integrationResponse,
        sourceSystem = "cantus"
    )
