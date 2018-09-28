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
import reactor.core.publisher.Mono
import org.springframework.web.reactive.function.client.ExchangeFilterFunction

enum class ServiceTypes {
    MOKEY, DOCKER, BOOBER, UNCLEMATT
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class TargetService(val value: ServiceTypes)

@Configuration
class ApplicationConfig(
    @Value("\${mokey.url}") val mokeyUrl: String,
    @Value("\${unclematt.url}") val uncleMattUrl: String,
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
    fun webClientDocker(): WebClient {

        // TODO: this should really use the built in trust?
        val sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()

        val httpConnector = ReactorClientHttpConnector { opt -> opt.sslContext(sslContext) }
        return webClientBuilder()
            .clientConnector(httpConnector)
            .build()
    }

    var logRequestFilter: ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
        logger.debug("method=${request.method()} url=${request.url()}")
        next.exchange(request)
    }

    val logResponseFilter = ExchangeFilterFunction.ofResponseProcessor { clientResponse ->
        logger.debug("Response status=${clientResponse.statusCode()}")
        Mono.just(clientResponse)
    }

    @Bean
    @TargetService(ServiceTypes.BOOBER)
    fun webClientBoober() = webClientBuilder().build()

    private fun webClientBuilder(): WebClient.Builder {

        return WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(exchangeStrategies())
            .filter(logRequestFilter)
            .filter(logResponseFilter)
            .clientConnector(clientConnector())
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

    private fun clientConnector() =
        ReactorClientHttpConnector { options ->
            options
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .compression(true)
                .afterNettyContextInit {
                    it.addHandlerLast(ReadTimeoutHandler(readTimeout.toLong(), TimeUnit.MILLISECONDS))
                }
        }
}
