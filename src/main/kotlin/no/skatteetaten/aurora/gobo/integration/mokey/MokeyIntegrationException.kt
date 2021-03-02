package no.skatteetaten.aurora.gobo.integration.mokey

import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.http.HttpStatus

class MokeyIntegrationException(
    message: String,
    status: HttpStatus? = null
) : SourceSystemException(
    message = message,
    code = status?.reasonPhrase ?: "",
    sourceSystem = "mokey"
)
