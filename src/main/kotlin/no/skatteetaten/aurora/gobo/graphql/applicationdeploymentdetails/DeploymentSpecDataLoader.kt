package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import com.fasterxml.jackson.databind.JsonNode
import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.boober.response
import org.springframework.stereotype.Component
import java.net.URL

@Component
class DeploymentSpecDataLoader(private val booberWebClient: BooberWebClient) :
    GoboDataLoader<URL, DeploymentSpec?>() {
    override suspend fun getByKeys(keys: Set<URL>, ctx: GoboGraphQLContext): Map<URL, DeploymentSpec?> {
        return keys.associateWith { url ->
            booberWebClient.get<JsonNode>(token = ctx.token(), url = url.toString()).response()
                .let { DeploymentSpec(it.toString()) }
        }
    }
}
