package no.skatteetaten.aurora.gobo

import org.springframework.hateoas.core.AnnotationRelProvider
import org.springframework.hateoas.hal.HalConfiguration
import org.springframework.hateoas.hal.Jackson2HalModule

fun createHalTestObjectMapper() = createObjectMapper().apply {
    setHandlerInstantiator(
        Jackson2HalModule.HalHandlerInstantiator(
            AnnotationRelProvider(),
            null,
            null,
            HalConfiguration()
        )
    )
}