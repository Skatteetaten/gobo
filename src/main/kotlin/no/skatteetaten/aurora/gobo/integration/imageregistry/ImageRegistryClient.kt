package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class ImageRegistryClient(
    @TargetService(ServiceTypes.DOCKER) val webClient: WebClient,
    val tokenProvider: TokenProvider
) {
    final inline fun <reified T : Any> getFromRegistry(registryMetadata: RegistryMetadata, apiUrl: String): T? {

        return try {
            webClient
                .get()
                .uri(apiUrl)
                .headers {
                    if (registryMetadata.authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                        it.set("Authorization", "Bearer ${tokenProvider.token}")
                    }
                }
                .retrieve()
                .bodyToMono<T>()
                .block()
        } catch (e: WebClientResponseException) {
            if (e.statusCode != HttpStatus.NOT_FOUND) {
                throw e
            }
            null
        }
    }
}