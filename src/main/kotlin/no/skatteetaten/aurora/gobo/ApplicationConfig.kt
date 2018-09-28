package no.skatteetaten.aurora.gobo

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.TimeUnit
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.core.publisher.Mono

enum class ServiceTypes {
    MOKEY, DOCKER, BOOBER, UNCLEMATT
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class TargetService(val value: ServiceTypes)

@Configuration
class ApplicationConfig(
    @Value("\${gobo.mokey.url}") val mokeyUrl: String,
    @Value("\${gobo.unclematt.url}") val uncleMattUrl: String,
    @Value("\${gobo.webclient.read-timeout:30000}") val readTimeout: Int,
    @Value("\${gobo.webclient.connection-timeout:30000}") val connectionTimeout: Int
) {

    private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)

    @Bean
    @Primary
    @TargetService(ServiceTypes.MOKEY)
    fun webClientMokey(): WebClient {

        logger.info("Configuring Mokey WebClient with baseUrl={}", mokeyUrl)

        return webClientBuilder()
            .baseUrl(mokeyUrl)
            .build()
    }

    @Bean
    @TargetService(ServiceTypes.UNCLEMATT)
    fun webClientUncleMatt(): WebClient {

        logger.info("Configuring UncleMatt WebClient with baseUrl={}", uncleMattUrl)

        return webClientBuilder()
            .baseUrl(uncleMattUrl)
            .build()
    }

    @Bean
    @TargetService(ServiceTypes.DOCKER)
    fun webClientDocker() = webClientBuilder(ssl = true).build()

    @Bean
    @TargetService(ServiceTypes.BOOBER)
    fun webClientBoober() = webClientBuilder().build()

    private fun webClientBuilder(ssl: Boolean = false): WebClient.Builder {

        return WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(exchangeStrategies())
            .filter(ExchangeFilterFunction.ofRequestProcessor {
                logger.debug("HttpRequest method=${it.method()} url=${it.url()}")
                Mono.just(it)
            })
            .clientConnector(clientConnector(ssl))
    }

    private fun exchangeStrategies(): ExchangeStrategies {
        val objectMapper = createObjectMapper()
        return ExchangeStrategies
            .builder()
            .codecs {
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
            }
            .build()
    }

    private fun clientConnector(ssl: Boolean = false) =
        ReactorClientHttpConnector { options ->
            if (ssl) {
                val sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()
                options.sslContext(sslContext)
            }

            options
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .compression(true)
                .afterNettyContextInit {
                    it.addHandlerLast(ReadTimeoutHandler(readTimeout.toLong(), TimeUnit.MILLISECONDS))
                }
        }
}
