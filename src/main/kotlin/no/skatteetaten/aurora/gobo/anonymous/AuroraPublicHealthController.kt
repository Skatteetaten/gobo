package no.skatteetaten.aurora.gobo.anonymous

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/public")
class AuroraPublicHealthController(
    @Value("\${openshift.cluster}") val cluster: String,
    val appliationService: ApplicationServiceBlocking
) {

    @GetMapping("/truesight")
    fun truesight(): List<TrueSightStatus> {

        return appliationService.getApplications(emptyList())
            .flatMap { it.applicationDeployments }
            .map {
                TrueSightStatus(
                    aktiv = it.status.code != "OFF",
                    registrering = TrueSightRegistrering(
                        tilbyderNavn = it.namespace,
                        helsetilstand = convertHealthCheckNames(it.status.code),
                        komponent = it.name,
                        hostOgPort = "openshift-$cluster"
                    )
                )
            }
    }

    fun convertHealthCheckNames(name: String) = when (name) {
        "HEALTHY" -> "FRISK"
        "OBSERVE" -> "TIL_OBSERVASJON"
        "DOWN" -> "STOPPET"
        "UNKNOWN" -> "UNKNOWN"
        else -> "NULL"
    }
}

data class TrueSightStatus(
    val aktiv: Boolean,
    val registrering: TrueSightRegistrering,
    val avhengighter: List<String> = emptyList()
)

data class TrueSightRegistrering(
    val tilbyderNavn: String,
    val helsetilstand: String,
    val komponent: String,
    val hostOgPort: String
)
