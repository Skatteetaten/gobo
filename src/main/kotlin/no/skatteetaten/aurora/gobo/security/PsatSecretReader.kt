package no.skatteetaten.aurora.gobo.security

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

/**
 * Component for reading the shared secret used for authentication with psat token. You may specify the shared secret directly using
 * the aurora.psat.token.value property, or specify a file containing the secret with the aurora.psat.token.location property.
 */
@Component
class PsatSecretReader(
    @Value("\${aurora.psat.token.location:}") private val secretLocation: String?,
    @Value("\${aurora.psat.token.value}:") private val secretValue: String?
) {

    val secret = initSecret(secretValue)

    private fun initSecret(secretValue: String?) =
        if (secretLocation.isNullOrEmpty() && secretValue.isNullOrEmpty()) {
            throw IllegalArgumentException("Either aurora.psat.token.location or aurora.psat.token.value must be specified")
        } else {
            if (secretValue.isNullOrEmpty()) {
                val secretFile = File(secretLocation).absoluteFile
                try {
                    logger.info("Reading token from file {}", secretFile.absolutePath)
                    secretFile.readText()
                } catch (e: IOException) {
                    throw IllegalStateException("Unable to read shared secret from specified location [${secretFile.absolutePath}]")
                }
            } else {
                secretValue
            }
        }
}
