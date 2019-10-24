package no.skatteetaten.aurora.gobo

import org.springframework.hateoas.mediatype.MessageResolver
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider
import org.springframework.hateoas.mediatype.hal.HalConfiguration
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule
import org.springframework.hateoas.server.core.AnnotationLinkRelationProvider

fun createHalTestObjectMapper() = createObjectMapper().apply {
    setHandlerInstantiator(
        Jackson2HalModule.HalHandlerInstantiator(
            AnnotationLinkRelationProvider(),
            DefaultCurieProvider.NONE,
            MessageResolver.DEFAULTS_ONLY,
            HalConfiguration()
        )
    )
}