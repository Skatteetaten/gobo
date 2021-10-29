package no.skatteetaten.aurora.gobo.integration.toxiproxy

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

class ToxiProxyService

@Service
class ToxiProxyToxicServiceReactive(
    @TargetService(ServiceTypes.BOOBER) private val webClient: WebClient
) : ToxiProxyToxicService

/*
    override suspend fun addToxiProxyToxic(environment: String, token: String) =
        webClient
            .post()
            .uri("/environments/{environment}", environment)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatusNotOk { status, body ->
                throw PhilIntegrationException(
                    message = "Request failed when deploying environment",
                    integrationResponse = body,
                    status = status
                )
            }
            .bodyToMono<List<DeploymentResource>>()
            .awaitFirstOrNull()
}
*/

interface ToxiProxyToxicService {
    suspend fun deployEnvironment(environment: String, token: String): List<DeploymentResource>? =
        integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Integration is disabled for this environment")
}
