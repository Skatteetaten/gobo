@file:JvmName("Main")

package no.skatteetaten.aurora.gobo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.core.context.SecurityContextHolder

@SpringBootApplication
class Gobo

fun main(args: Array<String>) {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
    runApplication<Gobo>(*args)
}
