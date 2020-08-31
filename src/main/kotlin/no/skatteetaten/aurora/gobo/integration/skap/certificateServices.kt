package no.skatteetaten.aurora.gobo.integration.skap

import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.RequiresSkap
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.application.Certificate
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
) {

    suspend fun getCertificates(): List<Certificate> =
        webClient
            .get()
            .uri("/certificate/list")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .awaitBody()
}

interface CertificateService {
    fun getCertificates(): List<Certificate> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Skap integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresSkap::class)
class CertificateServiceBlocking(private val certificateService: CertificateServiceReactive) : CertificateService {

    override fun getCertificates() = runBlocking { certificateService.getCertificates() }
}

@Service
@ConditionalOnMissingBean(RequiresSkap::class)
class CertificateServiceDisabled : CertificateService
