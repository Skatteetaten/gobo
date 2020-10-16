package no.skatteetaten.aurora.gobo.integration.skap

import no.skatteetaten.aurora.gobo.RequiresSkap
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.application.Certificate
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
@ConditionalOnBean(RequiresSkap::class)
class CertificateServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient
) : CertificateService {

    override suspend fun getCertificates(): List<Certificate> =
        webClient
            .get()
            .uri("/certificate/list")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .awaitBody()
}

interface CertificateService {
    suspend fun getCertificates(): List<Certificate> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Skap integration is disabled for this environment")
}

@Service
@ConditionalOnMissingBean(RequiresSkap::class)
class CertificateServiceDisabled : CertificateService
