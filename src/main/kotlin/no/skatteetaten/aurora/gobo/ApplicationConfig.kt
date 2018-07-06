package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation
import okhttp3.OkHttpClient
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.hal.Jackson2HalModule
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Configuration
@EnableCaching
class ApplicationConfig(
    @Value("\${mokey.url}") val mokeyUrl: String
) {

    @Bean
    fun webClient(): WebClient =
        WebClient
            .builder()
            .baseUrl(mokeyUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

    @Bean
    fun objectMapper() =
        ObjectMapper().apply {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(SerializationFeature.INDENT_OUTPUT, true)
            registerModules(JavaTimeModule(), Jackson2HalModule())
            registerKotlinModule()
        }

    @Bean
    fun dataLoaderRegistry(loaderList: List<DataLoader<*, *>>): DataLoaderRegistry {
        val registry = DataLoaderRegistry()
        loaderList.forEach {
            registry.register(it.toString(), it)
        }
        return registry
    }

    @Bean
    fun instrumentation(dataLoaderRegistry: DataLoaderRegistry) =
        DataLoaderDispatcherInstrumentation(dataLoaderRegistry)

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate =
        builder.requestFactory { createRequestFactory() }.build()
}

private fun createRequestFactory() = OkHttp3ClientHttpRequestFactory(
    OkHttpClient().newBuilder().sslSocketFactory(
        sslContext.socketFactory,
        TrustAllX509TrustManager
    ).build()
)

private val sslContext: SSLContext = SSLContext.getInstance("TLS")
    .apply { init(null, arrayOf(TrustAllX509TrustManager), SecureRandom()) }

private object TrustAllX509TrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
}
