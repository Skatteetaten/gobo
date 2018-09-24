package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentCommandResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraConfigRefResource
import no.skatteetaten.aurora.gobo.integration.mokey.GitInfoResource
import no.skatteetaten.aurora.gobo.integration.mokey.ImageDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.PodResourceResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusResource
import no.skatteetaten.aurora.gobo.integration.mokey.VersionResource
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeResult
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeStatus
import no.skatteetaten.aurora.gobo.integration.unclematt.Result
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.Status
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.Version
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import org.springframework.hateoas.Link
import java.time.Instant

data class ApplicationDeploymentResourceBuilder(val affiliation: String = "paas") {
    fun build(): ApplicationDeploymentResource =
        ApplicationDeploymentResource(
            "id",
            "name",
            affiliation,
            "environment",
            "namespace",
            StatusResource("code", "comment"),
            VersionResource("deployTag", "auroraVersion")
        ).apply {
            add(Link("http://ApplicationDeploymentDetails/1", "ApplicationDeploymentDetails"))
        }
}

data class ApplicationResourceBuilder(val name: String = "name") {

    fun build(): ApplicationResource =
        ApplicationResource(
            "id",
            name,
            listOf(ApplicationDeploymentResourceBuilder().build())
        )
}

data class ApplicationDeploymentBuilder(val affiliation: String = "paas") {

    fun build(): ApplicationDeployment =
        ApplicationDeployment(
            "id",
            "name",
            affiliation,
            "environment",
            "namespaceId",
            Status("code", "comment"),
            Version(ImageTag(ImageRepository("", "", ""), "deployTag"), "auroraVersion"),
            null
        )
}

class ApplicationDeploymentDetailsBuilder {

    fun build(): ApplicationDeploymentDetailsResource =
        ApplicationDeploymentDetailsResource(
            Instant.now(),
            GitInfoResource("123abc", Instant.now()),
            ImageDetailsResource(Instant.now(), "dockerImageReference"),
            listOf(
                PodResourceResource(
                    "name",
                    "status",
                    0,
                    true,
                    Instant.now()
                )
            ),
            ApplicationDeploymentCommandResource(
                emptyMap(),
                ApplicationDeploymentRefResource("environment", "application"),
                AuroraConfigRefResource("name", "refName")
            )
        ).apply { add(Link("http://ApplicationDeploymentDetails/1", "self")) }
}

class ProbeResultListBuilder {
    fun build(): List<ProbeResult> = listOf(
            ProbeResult(
                result = Result(
                    status = ProbeStatus.OPEN,
                    message = "Firewall open",
                    dnsname = "test.server",
                    resolvedIp = "192.168.1.1",
                    port = "80"
                ),
                podIp = "192.168.10.1",
                hostIp = "192.168.100.1"
            ),
            ProbeResult(
                result = Result(
                    status = ProbeStatus.CLOSED,
                    message = "Firewall closed",
                    dnsname = "test.server",
                    resolvedIp = "192.168.1.2",
                    port = "80"
                ),
                podIp = "192.168.10.2",
                hostIp = "192.168.100.2"
            ),
            ProbeResult(
                result = Result(
                    status = ProbeStatus.UNKNOWN,
                    message = "Unknown status",
                    dnsname = "test.server",
                    resolvedIp = "192.168.1.3",
                    port = "80"
                ),
                podIp = "192.168.10.3",
                hostIp = "192.168.100.3"
            )
    )
}
