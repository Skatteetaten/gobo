package no.skatteetaten.aurora.gobo.integration.cantus

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class CantusIntegrationException(
    message: String,
    integrationResponse: String = message,
    status: HttpStatus? = null
) :
    SourceSystemException(
        message = message,
        code = status?.reasonPhrase ?: "",
        integrationResponse = integrationResponse,
        sourceSystem = ServiceTypes.CANTUS
    )
