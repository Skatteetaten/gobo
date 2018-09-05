package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation
import no.skatteetaten.aurora.utils.createRequestFactory
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.hal.Jackson2HalModule
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ApplicationConfig(
    @Value("\${mokey.url}") val mokeyUrl: String
) {

    val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)

    @Bean
    fun webClient(): WebClient {

        logger.info("Configuring WebClient with baseUrl={}", mokeyUrl)

        return WebClient
            .builder()
            .baseUrl(mokeyUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

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