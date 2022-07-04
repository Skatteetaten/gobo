@file:JvmName("Main")

package no.skatteetaten.aurora.gobo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class Gobo

fun main(args: Array<String>) {
    runApplication<Gobo>(*args)
}
