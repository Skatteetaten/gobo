package no.skatteetaten.aurora.gobo.integration.imageregistry

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.function.Function
import java.util.function.Predicate

@Service
class ImageRegistryClient(
    @TargetService(ServiceTypes.DOCKER) val webClient: WebClient,
    val tokenProvider: TokenProvider
) {

    fun getTags(apiUrl: String, authenticationMethod: AuthenticationMethod): TagList? {
        return getFromRegistry(apiUrl, authenticationMethod)
    }

    fun getManifest(apiUrl: String, authenticationMethod: AuthenticationMethod): String? {
        return getFromRegistry(apiUrl, authenticationMethod)
    }

    private final inline fun <reified T : Any> getFromRegistry(
        apiUrl: String,
        authenticationMethod: AuthenticationMethod
    ): T? = webClient
        .get()
        .uri(apiUrl)
        .headers {
            if (authenticationMethod == AuthenticationMethod.KUBERNETES_TOKEN) {
                it.set("Authorization", "Bearer ${tokenProvider.token}")
            }
        }
        .retrieve()
        // TODO: It would be nice with a kotlin dsl over WebClient
        .onStatus(Predicate.isEqual<HttpStatus>(HttpStatus.NOT_FOUND), Function { Mono.empty() })
        .bodyToMono<T>()
        .block()
}

