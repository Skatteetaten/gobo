package no.skatteetaten.aurora.gobo.security

import java.io.File
import java.io.IOException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Component for reading the shared secret used for authentication. You may specify the shared secret directly using
 * the aurora.token.value property, or specify a file containing the secret with the aurora.token.location property.
 */
@Component
class SharedSecretReader(
    @Value("\${aurora.token.location:}") private val secretLocation: String?,
    @Value("\${aurora.token.value:}") private val secretValue: String?
) {

    val secret = initSecret(secretValue)

    private fun initSecret(secretValue: String?) =
        if (secretLocation.isNullOrEmpty() && secretValue.isNullOrEmpty()) {
            throw IllegalArgumentException("Either aurora.token.location or aurora.token.value must be specified")
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
