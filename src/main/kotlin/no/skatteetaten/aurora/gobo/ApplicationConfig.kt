package no.skatteetaten.aurora.gobo

import io.netty.channel.ChannelOption
import io.netty.channel.epoll.EpollChannelOption
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.infrastructure.ConditionalOnDatabaseUrl
import no.skatteetaten.aurora.gobo.infrastructure.ConditionalOnMissingDatabaseUrl
import no.skatteetaten.aurora.gobo.integration.skap.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.kubernetes.KubernetesReactorClient
import no.skatteetaten.aurora.kubernetes.config.ClientTypes
import no.skatteetaten.aurora.kubernetes.config.TargetClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.tcp.DefaultSslContextSpec
import reactor.netty.tcp.SslProvider
import java.time.Duration
import javax.annotation.PostConstruct

enum class ServiceTypes {
    MOKEY, BOOBER, UNCLEMATT, CANTUS, DBH, SKAP, HERKIMER, NAGHUB, GAVEL, PHIL, TOXI_PROXY, SPOTLESS
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

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty("integrations.spotless.url")
class RequiresSpotless

private val logger = KotlinLogging.logger {}

@Configuration
class ApplicationConfig(
    @Value("\${gobo.webclient.connection-timeout:15000}") val connectionTimeout: Int,
    @Value("\${gobo.webclient.response-timeout:60000}") val responseTimeout: Long,
    @Value("\${gobo.webclient.maxLifeTime:150000}") val maxLifeTime: Long,
    @Value("\${gobo.webclient.maxConnections:64}") val maxConnections: Int,
    @Value("\${gobo.coroutines.debug.enabled:false}") val coroutinesDebug: Boolean,
    private val sharedSecretReader: SharedSecretReader,
) {

    @PostConstruct
    fun initCoroutinesDebug() {
        if (coroutinesDebug) {
            logger.info("Coroutines debug enabled")
            @OptIn(ExperimentalCoroutinesApi::class)
            DebugProbes.install()
        }
    }

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

    @ConditionalOnBean(RequiresSpotless::class)
    @Bean
    @TargetService(ServiceTypes.SPOTLESS)
    fun webClientSpotless(
        @Value("\${integrations.spotless.url}") spotlessUrl: String,
        @TargetClient(ClientTypes.PSAT) psatKubernetesClient: KubernetesReactorClient
    ): WebClient {
        logger.info("Configuring Spotless WebClient with base Url={}", spotlessUrl)
        return psatKubernetesClient.webClient
            .mutate()
            .baseUrl(spotlessUrl)
            .filter(
                ExchangeFilterFunction.ofRequestProcessor { request ->
                    Mono.just(
                        ClientRequest.from(request)
                            .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer ${psatKubernetesClient.tokenFetcher.token("spotless")}"
                            )
                            .build()
                    )
                }
            )
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
        val httpClient = HttpClient.create(
            ConnectionProvider
                .builder("gobo-connection-provider")
                .metrics(true)
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(maxConnections * 2)
                .maxLifeTime(Duration.ofMillis(maxLifeTime))
                .maxIdleTime(Duration.ofMillis(maxLifeTime / 2))
                .evictInBackground(Duration.ofMillis(maxLifeTime * 2))
                .disposeTimeout(Duration.ofSeconds(10))
                .build()
        )
            .compress(true)
            // https://projectreactor.io/docs/netty/release/reference/index.html#connection-timeout
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(EpollChannelOption.TCP_KEEPIDLE, 150)
            .option(EpollChannelOption.TCP_KEEPINTVL, 15)
            .option(EpollChannelOption.TCP_KEEPCNT, 3)
            .responseTimeout(Duration.ofMillis(responseTimeout))

        if (ssl) {
            val sslProvider = SslProvider.builder().sslContext(
                DefaultSslContextSpec.forClient()
                    .configure { it.trustManager(InsecureTrustManagerFactory.INSTANCE) }
            ).build()
            httpClient.secure(sslProvider)
        }

        return ReactorClientHttpConnector(httpClient)
    }
}

@Configuration
@ConditionalOnDatabaseUrl
class GoboEnableDatabaseAutoConfiguration {
    init {
        logger.info { "Database integration enabled" }
    }
}

@Configuration
@ConditionalOnMissingDatabaseUrl
@EnableAutoConfiguration(exclude = [FlywayAutoConfiguration::class, DataSourceAutoConfiguration::class])
class GoboDisableDatabaseAutoConfiguration {
    init {
        logger.info { "Database integration disabled" }
    }
}
