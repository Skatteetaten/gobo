package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.hal.Jackson2HalModule
import org.springframework.web.client.RestTemplate

@Configuration
class ApplicationConfig : BeanPostProcessor {

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    @Bean
    fun objectMapper() =
        ObjectMapper().apply {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(SerializationFeature.INDENT_OUTPUT, true)
            registerModules(JavaTimeModule(), Jackson2HalModule())
            registerKotlinModule()
        }
}