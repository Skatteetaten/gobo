package no.skatteetaten.aurora.gobo.security

import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
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
    psatTokenConfig: PsatTokenConfig
) {
    val secret = initSecret(psatTokenConfig)

    private fun initSecret(psatTokenConfig: PsatTokenConfig): Map<String, String> {
        when {
            !psatTokenConfig.location.isNullOrEmpty() -> {
                val dir = File(psatTokenConfig.location)
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
            !psatTokenConfig.values.isNullOrEmpty() -> {
                psatTokenConfig.values.forEach {
                    if (it.value.isEmpty()) {
                        throw IllegalArgumentException("Missing value for specified key [${it.key}]")
                    }
                }
                return psatTokenConfig.values
            }
            else -> {
                throw IllegalArgumentException("Either aurora.psat.location or aurora.psat.values.<audience> must be specified")
            }
        }
    }
}

@ConfigurationProperties(prefix = "aurora.psat")
@ConstructorBinding
data class PsatTokenConfig(
    val values: Map<String, String>?,
    val location: String?
)
