package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component
import reactor.core.publisher.toMono
import java.net.URL

@Component
class DeploymentSpecDataLoader(
    private val booberWebClient: BooberWebClient,
    private val objectMapper: ObjectMapper
) : KeyDataLoader<URL, DeploymentSpec> {
    override fun getByKey(user: User, key: URL): Try<DeploymentSpec> {
        return Try.tryCall {
            booberWebClient.get<JsonNode>(user.token, key.toString())
                .toMono()
                .map { DeploymentSpec(jsonRepresentation = objectMapper.writeValueAsString(it)) }
                .block() ?: throw IllegalArgumentException("Empty DeploymentSpec with url=$key")
        }
    }
}
