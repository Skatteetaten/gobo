package no.skatteetaten.aurora.gobo.integration.dbh

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class DbhIntegrationException(
    message: String,
    integrationResponse: String = message,
    status: HttpStatus? = null
) : SourceSystemException(
    message = message,
    code = status?.reasonPhrase ?: "",
    integrationResponse = integrationResponse,
    sourceSystem = ServiceTypes.DBH
)
