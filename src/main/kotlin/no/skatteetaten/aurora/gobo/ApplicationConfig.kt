package no.skatteetaten.aurora.gobo

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.skap.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.SslProvider
import java.util.concurrent.TimeUnit

enum class ServiceTypes {
    MOKEY, BOOBER, UNCLEMATT, CANTUS, DBH, SKAP, HERKIMER, NAGHUB, GAVEL, PHIL
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

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.herkimer.url")
class RequiresHerkimer

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.naghub.url")
class RequiresNagHub

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.gavel.url")
class RequiresGavel

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.phil.url")
class RequiresPhil

private val logger = KotlinLogging.logger {}

@Configuration
class ApplicationConfig(
    @Value("\${gobo.webclient.read-timeout:30000}") val readTimeout: Long,
    @Value("\${gobo.webclient.write-timeout:30000}") val writeTimeout: Long,
    @Value("\${gobo.webclient.connection-timeout:30000}") val connectionTimeout: Int,
    @Value("\${spring.application.name}") val applicationName: String,
    private val sharedSecretReader: SharedSecretReader
) {

    @Bean
    @ConditionalOnProperty(value = ["management.endpoint.httptrace.enabled"], havingValue = "true")
    fun inMemoryHttpTraceRepository(): InMemoryHttpTraceRepository {
        logger.info("In-memory HTTP trace repository enabled")
        return InMemoryHttpTraceRepository()
    }

    @Bean
    @TargetService(ServiceTypes.MOKEY)
    fun webClientMokey(@Value("\${integrations.mokey.url}") mokeyUrl: String, builder: WebClient.Builder): WebClient {
        logger.info("Configuring Mokey WebClient with baseUrl={}", mokeyUrl)
        return builder.init().baseUrl(mokeyUrl).build()
    }

    @Bean
    @TargetService(ServiceTypes.UNCLEMATT)
    fun webClientUncleMatt(
        @Value("\${integrations.unclematt.url}") uncleMattUrl: String,
        builder: WebClient.Builder
    ): WebClient {
        logger.info("Configuring UncleMatt WebClient with baseUrl={}", uncleMattUrl)
        return builder.init().baseUrl(uncleMattUrl).build()
    }

    @Bean
    @TargetService(ServiceTypes.CANTUS)
    fun webClientCantus(
        @Value("\${integrations.cantus.url}") cantusUrl: String,
        builder: WebClient.Builder
    ): WebClient {
        logger.info("Configuring Cantus WebClient with base Url={}", cantusUrl)
        return builder.init().baseUrl(cantusUrl).build()
    }

    @ConditionalOnBean(RequiresGavel::class)
    @Bean
    @TargetService(ServiceTypes.GAVEL)
    fun webClientGavel(
        @Value("\${integrations.gavel.url}") gavelUrl: String,
        builder: WebClient.Builder
    ): WebClient {
        logger.info("Configuring Gavel WebClient with base Url={}", gavelUrl)
        return builder.init().baseUrl(gavelUrl).build()
    }

    @ConditionalOnBean(RequiresNagHub::class)
    @Bean
    @TargetService(ServiceTypes.NAGHUB)
    fun webClientNagHub(
        @Value("\${integrations.naghub.url}") nagHubUrl: String,
        builder: WebClient.Builder
    ): WebClient {
        logger.info("Configuring Nag-Hub WebClient with base Url={}", nagHubUrl)
        return builder.init()
            .baseUrl(nagHubUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .build()
    }

    @ConditionalOnBean(RequiresHerkimer::class)
    @Bean
    @TargetService(ServiceTypes.HERKIMER)
    fun webClientHerkimer(
        @Value("\${integrations.herkimer.url}") herkimerUrl: String,
        builder: WebClient.Builder
    ): WebClient {
        logger.info("Configuring Herkimer WebClient with base Url={}", herkimerUrl)
        return builder.init()
            .baseUrl(herkimerUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .build()
    }

    @Bean
    @TargetService(ServiceTypes.BOOBER)
    fun webClientBoober(builder: WebClient.Builder) = builder.init().build()

    @ConditionalOnBean(RequiresSkap::class)
    @Bean
    @TargetService(ServiceTypes.SKAP)
    fun webClientSkap(@Value("\${integrations.skap.url}") skapUrl: String, builder: WebClient.Builder) =
        builder.init().baseUrl(skapUrl).build()

    @ConditionalOnBean(RequiresDbh::class)
    @Bean
    @TargetService(ServiceTypes.DBH)
    fun webClientDbh(@Value("\${integrations.dbh.url}") dbhUrl: String, builder: WebClient.Builder) =
        builder.init().baseUrl(dbhUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}").build()

    @ConditionalOnBean(RequiresPhil::class)
    @Bean
    @TargetService(ServiceTypes.PHIL)
    fun webClientPhil(
        @Value("\${integrations.phil.url}") philUrl: String,
        builder: WebClient.Builder
    ): WebClient {
        logger.info("Configuring Phil WebClient with base Url={}", philUrl)
        return builder.init()
            .baseUrl(philUrl)
            .build()
    }

    fun WebClient.Builder.init() =
        this.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(
                ExchangeFilterFunction.ofRequestProcessor {
                    logger.debug { "HttpRequest method=${it.method()} url=${it.url()}" }
                    it.toMono()
                }
            )
            .clientConnector(clientConnector())

    private fun clientConnector(ssl: Boolean = false): ReactorClientHttpConnector {
        val httpClient =
            HttpClient.create().compress(true)
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
