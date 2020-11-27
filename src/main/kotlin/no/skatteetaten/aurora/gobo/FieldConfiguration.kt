package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.infrastructure.InternalFieldConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = arrayOf(InternalFieldConfiguration::class))
class FieldConfiguration
