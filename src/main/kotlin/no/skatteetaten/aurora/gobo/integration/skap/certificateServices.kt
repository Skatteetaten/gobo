package no.skatteetaten.aurora.gobo.integration.skap

import no.skatteetaten.aurora.gobo.RequiresSkap
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Service
@ConditionalOnBean(RequiresSkap::class)
class CertificateServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient
) {

    fun getCertificates(): Mono<List<Certificate>> =
        webClient
            .get()
            .uri("/certificate/list")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()
}

interface CertificateService {
    fun getCertificates(): List<Certificate> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Skap integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresSkap::class)
class CertificateServiceBlocking(private val certificateService: CertificateServiceReactive) : CertificateService {

    override fun getCertificates() = certificateService.getCertificates().blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockNonNullWithTimeout() = this.blockNonNullAndHandleError(Duration.ofSeconds(30), "skap")
}

@Service
@ConditionalOnMissingBean(RequiresSkap::class)
class CertificateServiceDisabled : CertificateService
