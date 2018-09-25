package no.skatteetaten.aurora.gobo

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
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
    @Value("\${unclematt.url}") val uncleMattUrl: String
) {

    private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)

    @Bean
    @Primary
    @TargetService(ServiceTypes.MOKEY)
    fun webClientMokey(): WebClient {

        logger.info("Configuring WebClient with baseUrl={}", mokeyUrl)

        val objectMapper = createObjectMapper()
        val strategies = ExchangeStrategies
            .builder()
            .codecs {
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
            }
            .build()

        return createStandardWebClientBuilder()
            .exchangeStrategies(strategies)
            .baseUrl(mokeyUrl)
            .build()
    }

    @Bean
    @TargetService(ServiceTypes.UNCLEMATT)
    fun webClientUncleMatt(): WebClient {

        logger.info("Configuring WebClient with baseUrl={}", uncleMattUrl)

        val objectMapper = createObjectMapper()
        val strategies = ExchangeStrategies
            .builder()
            .codecs {
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
            }
            .build()

        return createStandardWebClientBuilder()
            .exchangeStrategies(strategies)
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
        return createStandardWebClientBuilder()
            .clientConnector(httpConnector)
            .build()
    }

    @Bean
    @TargetService(ServiceTypes.BOOBER)
    fun webClientBoober() = createStandardWebClientBuilder().build()

    private fun createStandardWebClientBuilder() =
        WebClient.builder().defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
}
