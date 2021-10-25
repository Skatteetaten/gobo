package no.skatteetaten.aurora.gobo.integration.skap

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import no.skatteetaten.aurora.gobo.RequiresSkap
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.security.SharedSecretReader

@Service
@ConditionalOnBean(RequiresSkap::class)
class WebsealServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient,
    @Value("\${openshift.cluster}") val cluster: String,
) : WebsealService {

    override suspend fun getStates(): List<WebsealStateResource> {
        val clustersToExclude = listOf("utv", "test", "prod")

        return webClient
            .get()
            .uri("/webseal/v3") {
                if (!clustersToExclude.contains(cluster)) it.queryParam("clusterId", cluster).build() else it.build()
            }
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .awaitBody()
    }
}

interface WebsealService {
    suspend fun getStates(): List<WebsealStateResource> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Skap integration is disabled for this environment")
}

@Service
@ConditionalOnMissingBean(RequiresSkap::class)
class WebsealServiceDisabled : WebsealService
