package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ApplicationDeploymentFilterService(private val booberWebClient: BooberWebClient) {

    fun getFilters(token: String): List<ApplicationDeploymentFilterResource> =
        booberWebClient.get<ApplicationDeploymentFilterResource>(
            token,
            "/v1/users/annotations/applicationDeploymentFilters"
        ).collectList().blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockNonNullWithTimeout() = this.blockNonNullAndHandleError(Duration.ofSeconds(30))
}

data class ApplicationDeploymentFilterResource(
    val name: String,
    val affiliation: String,
    val applications: List<String> = emptyList(),
    val environments: List<String> = emptyList()
)