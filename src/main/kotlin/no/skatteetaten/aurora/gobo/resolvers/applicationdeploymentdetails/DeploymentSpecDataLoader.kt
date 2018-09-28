package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
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
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.toMono
import java.net.URL

@Component
class DeploymentSpecDataLoader(
    @TargetService(ServiceTypes.BOOBER) private val webClient: WebClient,
    private val userService: UserService,
    private val configService: AuroraConfigService
) : KeysDataLoader<URL, Try<DeploymentSpec>> {

    private val logger: Logger = LoggerFactory.getLogger(DeploymentSpecDataLoader::class.java)

    override fun getByKeys(keys: List<URL>): List<Try<DeploymentSpec>> {

        logger.debug("Loading ${keys.size} DeploymentSpecs from boober (${keys.toSet().size} unique)")

        val token = userService.getToken()

        val sw = StopWatch()
        val specs: List<Try<DeploymentSpec>> = sw.time("Fetch ${keys.size} DeploymentSpecs") {
            keys.map { url ->
                Try.tryCall {
                    configService.get<String>(userService.getToken(), url.toString())
                        .toMono()
                        .map { DeploymentSpec(jsonRepresentation = it) }
                        .block() ?: throw IllegalArgumentException("Empty DeploymentSpec with url=$url")
                }
            }
        }

        logger.debug(sw.logLine)
        return specs
    }
}
