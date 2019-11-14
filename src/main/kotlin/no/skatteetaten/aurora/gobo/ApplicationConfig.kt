package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.math.min
import mu.KotlinLogging
import no.skatteetaten.aurora.filter.logging.AuroraHeaderFilter
import no.skatteetaten.aurora.filter.logging.RequestKorrelasjon
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.SslProvider

val HEADER_KLIENTID = "KlientID"

enum class ServiceTypes {
    MOKEY, BOOBER, UNCLEMATT, CANTUS, DBH, SKAP
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class TargetService(val value: ServiceTypes)

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.dbh.url")
class RequiresDbh

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.skap.url")
class RequiresSkap

private val logger = KotlinLogging.logger {}

@Configuration
class ApplicationConfig(
    @Value("\${gobo.webclient.read-timeout:30000}") val readTimeout: Long,
    @Value("\${gobo.webclient.write-timeout:30000}") val writeTimeout: Long,
    @Value("\${gobo.webclient.connection-timeout:30000}") val connectionTimeout: Int,
    @Value("\${spring.application.name}") val applicationName: String,
    val objectMapper: ObjectMapper,
    @Value("\${management.endpoints.web.exposure.include:}") val env: String?
) {

    @PostConstruct
    fun init() {
        logger.info("env: $env")
    }

    @Bean
    @Primary
    @TargetService(ServiceTypes.MOKEY)
    fun webClientMokey(@Value("\${integrations.mokey.url}") mokeyUrl: String): WebClient {
        logger.info("Configuring Mokey WebClient with baseUrl={}", mokeyUrl)
        return webClientBuilder().baseUrl(mokeyUrl).build()
    }

    @Bean
    @TargetService(ServiceTypes.UNCLEMATT)
    fun webClientUncleMatt(@Value("\${integrations.unclematt.url}") uncleMattUrl: String): WebClient {
        logger.info("Configuring UncleMatt WebClient with baseUrl={}", uncleMattUrl)
        return webClientBuilder().baseUrl(uncleMattUrl).build()
    }

    @Bean
    @TargetService(ServiceTypes.CANTUS)
    fun webClientCantus(@Value("\${integrations.cantus.url}") cantusUrl: String): WebClient {
        logger.info("Configuring Cantus WebClient with base Url={}", cantusUrl)

        return webClientBuilder()
            .baseUrl(cantusUrl)
            .build()
    }

    @Bean
    @TargetService(ServiceTypes.BOOBER)
    fun webClientBoober() = webClientBuilder().build()

    @ConditionalOnBean(RequiresSkap::class)
    @Bean
    @TargetService(ServiceTypes.SKAP)
    fun webClientSkap(@Value("\${integrations.skap.url}") skapUrl: String) =
        webClientBuilder().baseUrl(skapUrl).build()

    @ConditionalOnBean(RequiresDbh::class)
    @Bean
    @TargetService(ServiceTypes.DBH)
    fun webClientDbh(@Value("\${integrations.dbh.url}") dbhUrl: String) = webClientBuilder().baseUrl(dbhUrl).build()

    fun webClientBuilder(ssl: Boolean = false) =
        WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HEADER_KLIENTID, applicationName)
            .defaultHeader(AuroraHeaderFilter.KORRELASJONS_ID, RequestKorrelasjon.getId())
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
                        connection.addHandlerLast(ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        connection.addHandlerLast(WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
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
