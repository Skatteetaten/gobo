package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class BooberIntegrationException(
    response: Response<*>,
    status: HttpStatus? = null
) : SourceSystemException(
    message = response.message,
    code = status?.reasonPhrase ?: "",
    sourceSystem = "boober"
)
