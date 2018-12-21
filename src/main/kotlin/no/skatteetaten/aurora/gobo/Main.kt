@file:JvmName("Main")

package no.skatteetaten.aurora.gobo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Gobo

fun main(args: Array<String>) {
    runApplication<Gobo>(*args)
}