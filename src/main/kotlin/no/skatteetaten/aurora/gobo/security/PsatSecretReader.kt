package no.skatteetaten.aurora.gobo.security

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

/**
 * Component for reading the shared secret used for authentication with psat token. You may specify the shared secret directly using
 * the aurora.psat.values.<audience> property, or specify a directory containing files with secrets for psat with the aurora.psat.location property.
 *
 * Examples:
 * aurora.psat.location: /dir/to/psat
 * aurora.psat.values.spotless: <token-value>
 */
@Component
class PsatSecretReader(
    @Value("\${aurora.psat.location:}") private val secretLocation: String?,
    secretValues: PsatTokenValues
) {

    val secret = initSecret(secretValues)

    private fun initSecret(secretValues: PsatTokenValues): Map<String, String> {
        if (secretLocation.isNullOrEmpty() && secretValues.values.isNullOrEmpty()) {
            throw IllegalArgumentException("Either aurora.psat.location or aurora.psat.values.<audience> must be specified")
        }
        if (secretValues.values.isNullOrEmpty()) {
            val dir = File(this.secretLocation!!)
            if (!dir.exists()) {
                throw IllegalArgumentException("The directory [${dir.absolutePath}] does not exist")
            }

            return dir.walk()
                .filter { !it.isDirectory }
                .filter { !it.isHidden }
                .map { it.canonicalFile }
                .map {
                    try {
                        logger.info("Reading token from file {}", it.absolutePath)
                        it.name to it.readText()
                    } catch (e: IOException) {
                        throw IllegalStateException("Unable to read psat secret from specified location [${it.absolutePath}]")
                    }
                }
                .toMap()
        }
        secretValues.values!!.forEach {
            if (it.value.isEmpty()) {
                throw IllegalArgumentException("Missing value for specified key [${it.key}]")
            }
        }
        return secretValues.values ?: emptyMap()
    }
}

@Configuration
@ConfigurationProperties(prefix = "aurora.psat")
class PsatTokenValues(
    var values: Map<String, String>? = null
)
