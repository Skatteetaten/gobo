package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class BooberIntegrationException(
    response: Response<*>,
    status: HttpStatus? = null
) : SourceSystemException(
    message = response.message,
    integrationResponse = response.toString(),
    code = status?.reasonPhrase ?: "",
    sourceSystem = ServiceTypes.BOOBER
)
