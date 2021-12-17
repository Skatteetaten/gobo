package no.skatteetaten.aurora.gobo.integration.toxiproxy

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class ToxiProxyIntegrationException(
    message: String,
    integrationResponse: String = message,
    status: HttpStatus? = null
) :
    SourceSystemException(
        message = message,
        code = status?.reasonPhrase ?: "",
        integrationResponse = integrationResponse,
        sourceSystem = ServiceTypes.TOXI_PROXY
    )
