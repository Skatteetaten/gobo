package no.skatteetaten.aurora.gobo.integration.imageregistry

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

@Component
class TokenProvider(@Value("\${tokenLocation:/var/run/secrets/kubernetes.io/serviceaccount/token}") private val tokenLocation: String) {

    val token: String get() = File(tokenLocation).readText()
}