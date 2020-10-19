package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import com.fasterxml.jackson.databind.JsonNode
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.AccessDeniedException
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.boober.response
import org.springframework.stereotype.Component
import java.net.URL

@Component
class DeploymentSpecDataLoader(private val booberWebClient: BooberWebClient) :
    KeyDataLoader<URL, DeploymentSpec> {
    override suspend fun getByKey(key: URL, context: GoboGraphQLContext): DeploymentSpec {
        context.token ?: throw AccessDeniedException("Token required")
        return booberWebClient.get<JsonNode>(token = context.token, url = key.toString()).response()
            .let { DeploymentSpec(it.toString()) }
    }
}
