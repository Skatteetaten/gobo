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
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.SslProvider
import java.util.concurrent.TimeUnit
import kotlin.math.min

enum class ServiceTypes {
    MOKEY, DOCKER, BOOBER, UNCLEMATT, CANTUS, DBH
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class TargetService(val value: ServiceTypes)

@Configuration
class ApplicationConfig(
    @Value("\${gobo.mokey.url}") val mokeyUrl: String,
    @Value("\${gobo.unclematt.url}") val uncleMattUrl: String,
    @Value("\${gobo.dbh.url}") val dbhUrl: String,
    @Value("\${gobo.cantus.url}") val cantusUrl: String,
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
    @TargetService(ServiceTypes.CANTUS)
    fun webClientCantus(): WebClient {
        logger.info("Configuring Cantus WebClient with base Url={}", cantusUrl)

        return webClientBuilder()
            .baseUrl(cantusUrl)
            .build()
    }

    @Bean
    @TargetService(ServiceTypes.DOCKER)
    fun webClientDocker() = webClientBuilder(ssl = true).build()

    @Bean
    @TargetService(ServiceTypes.BOOBER)
    fun webClientBoober() = webClientBuilder().build()

    @Bean
    @TargetService(ServiceTypes.DBH)
    fun webClientDbh() = webClientBuilder().baseUrl(dbhUrl).build()

    fun webClientBuilder(ssl: Boolean = false) =
        WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(exchangeStrategies())
            .filter(ExchangeFilterFunction.ofRequestProcessor {
                val bearer = it.headers()[HttpHeaders.AUTHORIZATION]?.firstOrNull()?.let { token ->
                    val t = token.substring(0, min(token.length, 11)).replace("Bearer", "")
                    "bearer=$t"
                } ?: ""
                logger.debug("HttpRequest method=${it.method()} url=${it.url()} $bearer")
                it.toMono()
            })
            .clientConnector(clientConnector(ssl))

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

    private fun clientConnector(ssl: Boolean = false): ReactorClientHttpConnector {
        val httpClient = HttpClient.create().compress(true)
            .tcpConfiguration {
                it.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                    .doOnConnected { connection ->
                        connection.addHandlerLast(ReadTimeoutHandler(readTimeout.toLong(), TimeUnit.MILLISECONDS))
                    }
            }

        if (ssl) {
            val sslProvider = SslProvider.builder().sslContext(
                SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
            ).defaultConfiguration(SslProvider.DefaultConfigurationType.NONE).build()
            httpClient.tcpConfiguration {
                it.secure(sslProvider)
            }
        }

        return ReactorClientHttpConnector(httpClient)
    }
}
