package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.hal.Jackson2HalModule
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ApplicationConfig(
    @Value("\${mokey.url}") val mokeyUrl: String
) : BeanPostProcessor {

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
}