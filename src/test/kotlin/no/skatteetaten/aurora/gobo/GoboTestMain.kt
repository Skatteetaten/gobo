package no.skatteetaten.aurora.gobo

import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newPod
import io.kubernetes.client.PortForward
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.util.Config
import io.kubernetes.client.util.Streams
import io.netty.util.ResourceLeakDetector
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.PROFILE_LOCAL_OCP04
import no.skatteetaten.aurora.kubernetes.KubernetesReactorClient
import no.skatteetaten.aurora.kubernetes.config.kubernetesToken
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.ServerSocket
import javax.annotation.PostConstruct
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

data class PortForwardInput(val namespace: String, val application: String, val localPort: Int)

@Profile(PROFILE_LOCAL_OCP04)
@Component
class MokeyPortForward(val kubernetesClient: KubernetesReactorClient) {

    private val targetPort = 8080

    @PostConstruct
    fun init() {
        Configuration.setDefaultApiClient(Config.defaultClient())
        listOf(
            PortForwardInput("aup", "mokey", 9999),
            PortForwardInput("aup", "dbh", 9998)
        ).forEach {
            portForward(it)
        }
    }

    fun portForward(input: PortForwardInput) {
        val pod = kubernetesClient.getMany(
            resource =
            newPod {
                metadata {
                    namespace = input.namespace
                    labels = mapOf("app" to input.application)
                }
            },
            token = kubernetesToken()
        ).block()?.first() ?: throw IllegalStateException("Could not find ${input.application} pod")

        val podName = pod.metadata.name
        logger.info { "Connecting to $podName" }
        val forward = PortForward().forward(input.namespace, podName, listOf(targetPort))
        logger.info("Port-forwarding for $podName has started")
        val server = ServerSocket(input.localPort)

        thread {
            val socket = server.accept()
            logger.info("Connected to ${input.application}")

            thread {
                runCatching {
                    Streams.copy(forward.getInputStream(targetPort), socket.getOutputStream())
                }.onFailure {
                    logger.error(it) { "Exception when reading from ${input.application}" }
                }
            }

            thread {
                runCatching {
                    Streams.copy(socket.getInputStream(), forward.getOutboundStream(targetPort))
                }.onFailure {
                    logger.error(it) { "Exception when reading from localhost" }
                }
            }
        }
    }
}

@Profile(PROFILE_LOCAL_OCP04)
@SpringBootApplication
class GoboTestMain

fun main(args: Array<String>) {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
    runApplication<GoboTestMain>(*args) {
        setAdditionalProfiles(PROFILE_LOCAL_OCP04)
    }
}
