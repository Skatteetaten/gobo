package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.security.UserService
import no.skatteetaten.aurora.utils.logLine
import no.skatteetaten.aurora.utils.time
import org.dataloader.Try
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import reactor.core.publisher.toMono
import java.net.URL

@Component
class DeploymentSpecDataLoader(
    private val userService: UserService,
    private val configService: AuroraConfigService,
    private val objectMapper: ObjectMapper
) : KeysDataLoader<URL, Try<DeploymentSpec>> {

    private val logger: Logger = LoggerFactory.getLogger(DeploymentSpecDataLoader::class.java)

    override fun getByKeys(keys: List<URL>): List<Try<DeploymentSpec>> {

        logger.debug("Loading ${keys.size} DeploymentSpecs from boober (${keys.toSet().size} unique)")

        val sw = StopWatch()
        val specs: List<Try<DeploymentSpec>> = sw.time("Fetch ${keys.size} DeploymentSpecs") {
            keys.map { url ->
                Try.tryCall {
                    configService.get<JsonNode>(userService.getToken(), url.toString())
                        .toMono()
                        .map { DeploymentSpec(jsonRepresentation = objectMapper.writeValueAsString(it)) }
                        .block() ?: throw IllegalArgumentException("Empty DeploymentSpec with url=$url")
                }
            }
        }

        logger.debug(sw.logLine)
        return specs
    }
}
