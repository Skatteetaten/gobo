package no.skatteetaten.aurora.gobo

import org.springframework.hateoas.core.AnnotationRelProvider
import org.springframework.hateoas.hal.HalConfiguration
import org.springframework.hateoas.hal.Jackson2HalModule

fun createTestObjectMapper() = createObjectMapper().apply {
    setHandlerInstantiator(
        Jackson2HalModule.HalHandlerInstantiator(
            AnnotationRelProvider(),
            null,
            null,
            HalConfiguration()
        )
    )
}