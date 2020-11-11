@file:JvmName("Main")

package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.webflux.config.WebFluxStarterApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [WebFluxStarterApplicationConfig::class])
@EnableScheduling
class Gobo

fun main(args: Array<String>) {
    runApplication<Gobo>(*args)
}
