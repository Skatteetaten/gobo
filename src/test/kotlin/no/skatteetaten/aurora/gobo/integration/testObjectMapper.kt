package no.skatteetaten.aurora.gobo.integration

import com.fasterxml.jackson.databind.ObjectMapper
import no.skatteetaten.aurora.gobo.createObjectMapper
import org.springframework.hateoas.core.AnnotationRelProvider
import org.springframework.hateoas.hal.HalConfiguration
import org.springframework.hateoas.hal.Jackson2HalModule

fun createTestHateoasObjectMapper(): ObjectMapper {
    return createObjectMapper().apply {
        setHandlerInstantiator(
            Jackson2HalModule.HalHandlerInstantiator(
                AnnotationRelProvider(),
                null,
                null,
                HalConfiguration()
            )
        )
    }
}
