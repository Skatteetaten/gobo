package no.skatteetaten.aurora.gobo.integration.skap

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.certificate.Certificate
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class CertificateService(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient
) {

    companion object {
        const val HEADER_AURORA_TOKEN = "aurora-token"
    }

    fun getCertificates(): Mono<List<Certificate>> =
        webClient
            .get()
            .uri("/certificate/list")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()
}

@Service
class CertificateServiceBlocking(private val certificateService: CertificateService) {

    fun getCertificates() = certificateService.getCertificates().blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockNonNullWithTimeout() = this.blockNonNullAndHandleError(Duration.ofSeconds(30), "skap")
}
