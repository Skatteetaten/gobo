package no.skatteetaten.aurora.gobo.integration.herkimer

import org.springframework.http.HttpStatus
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.integration.SourceSystemException

class HerkimerIntegrationException(
    message: String,
    integrationResponse: String = message,
    status: HttpStatus? = null
) :
    SourceSystemException(
        message = message,
        code = status?.reasonPhrase ?: "",
        integrationResponse = integrationResponse,
        sourceSystem = ServiceTypes.HERKIMER
    )
