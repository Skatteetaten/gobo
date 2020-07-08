package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

fun testObjectMapper() = jacksonObjectMapper().apply {
    registerModule(JavaTimeModule())
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

@TestConfiguration
class TestConfig {
    @Bean
    fun objectMapper() = testObjectMapper()

    @Bean
    fun webClientBuilder() = WebClient.builder()
}
