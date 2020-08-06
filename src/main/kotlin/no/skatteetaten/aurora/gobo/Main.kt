@file:JvmName("Main")

package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.webflux.config.WebFluxStarterApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [WebFluxStarterApplicationConfig::class])
class Gobo

fun main(args: Array<String>) {
    runApplication<Gobo>(*args)
}
