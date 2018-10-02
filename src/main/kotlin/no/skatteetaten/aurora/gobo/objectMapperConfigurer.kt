package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.hateoas.hal.Jackson2HalModule

fun createObjectMapper() =
    ObjectMapper().apply {
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(SerializationFeature.INDENT_OUTPUT, true)
        registerModules(JavaTimeModule(), Jackson2HalModule())
        registerKotlinModule()
        enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
