package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration

@Service
class ApplicationDeploymentService(private val booberWebClient: BooberWebClient) {

    fun deleteApplicationDeployment(
        token: String,
        input: DeleteApplicationDeploymentInput
    ): Boolean {
        booberWebClient.post<DeleteApplicationDeploymentResponse>(
            url = "/v1/applicationdeployment/delete",
            token = token,
            body = input
        ).toMono().blockNonNullWithTimeout()
        return true
    }

    private fun <T> Mono<T>.blockNonNullWithTimeout() =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "boober")
}

data class ApplicationRef(val namespace: String, val name: String)

data class DeleteApplicationDeploymentInput(val namespace: String, val name: String)

data class DeleteApplicationDeploymentResponse(
    val applicationRef: ApplicationRef,
    val success: Boolean = true,
    val message: String = ""
)