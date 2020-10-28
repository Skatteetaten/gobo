package no.skatteetaten.aurora.gobo

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties("integrations")
@Component
class AuroraIntegration(val docker: Map<String, DockerRegistry>) {

    enum class AuthType {
        None, Basic, Bearer
    }

    class DockerRegistry {
        var url: String? = null
        var guiUrlPattern: String? = null
        var auth = AuthType.None
        var isHttps = true
        var isReadOnly = true
        var isEnabled = true
    }
}
