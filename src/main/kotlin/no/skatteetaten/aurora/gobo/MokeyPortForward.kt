package no.skatteetaten.aurora.gobo

import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newPod
import io.kubernetes.client.PortForward
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.util.Config
import io.kubernetes.client.util.Streams
import mu.KotlinLogging
import no.skatteetaten.aurora.kubernetes.KubernetesReactorClient
import no.skatteetaten.aurora.kubernetes.config.kubernetesToken
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.lang.IllegalStateException
import java.net.ServerSocket
import javax.annotation.PostConstruct
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

@Profile("local-ocp04")
@Component
class MokeyPortForward(val kubernetesClient: KubernetesReactorClient) {

    private val localPort = 9999
    private val targetPort = 8080

    @PostConstruct
    fun portForward() {
        Configuration.setDefaultApiClient(Config.defaultClient())

        val pod = kubernetesClient.getMany(
            resource =
            newPod {
                metadata {
                    namespace = "aup"
                    labels = mapOf("app" to "mokey")
                }
            },
            token = kubernetesToken()
        ).block()?.first() ?: throw IllegalStateException("Could not find mokey pod")

        val podName = pod.metadata.name
        val forward = PortForward().forward("aup", podName, listOf(targetPort))
        logger.info("Port-forwarding for $podName has started")
        val server = ServerSocket(localPort)

        thread {
            val socket = server.accept()
            logger.info("Connected to mokey")

            thread {
                runCatching {
                    Streams.copy(forward.getInputStream(targetPort), socket.getOutputStream())
                }.onFailure {
                    logger.error(it) { "Exception when reading from mokey" }
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
