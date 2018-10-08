package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentCommandResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraConfigRefResource
import no.skatteetaten.aurora.gobo.integration.mokey.GitInfoResource
import no.skatteetaten.aurora.gobo.integration.mokey.HttpResponseResource
import no.skatteetaten.aurora.gobo.integration.mokey.ImageDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ManagementResponsesResource
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
import org.intellij.lang.annotations.Language
import org.springframework.hateoas.Link
import java.time.Instant

val defaultInstant = Instant.parse("2018-01-01T00:00:01Z")

@Language("JSON")
val infoResponseJson: String = """{
  "git": {
    "build.time": "$defaultInstant",
    "commit.time": "$defaultInstant",
    "commit.id.abbrev": ""
  },
 "podLinks": {
    "metrics": "http://localhost"
  }
}"""

@Language("JSON")
val healthResponseJson: String = """{"status": "UP"}"""

data class ApplicationDeploymentResourceBuilder(val affiliation: String = "paas") {
    fun build(): ApplicationDeploymentResource =
        ApplicationDeploymentResource(
            identifier = "id",
            name = "name",
            affiliation = affiliation,
            environment = "environment",
            namespace = "namespace",
            status = StatusResource("code", "comment"),
            version = VersionResource("deployTag", "auroraVersion", "releaseTo"),
            dockerImageRepo = "dockerImageRepo",
            time = Instant.EPOCH
        ).apply {
            add(Link("http://ApplicationDeploymentDetails/1", "ApplicationDeploymentDetails"))
            add(Link("http://Application/1", "Application"))
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
            id = "id",
            name = "name",
            affiliationId = affiliation,
            environment = "environment",
            namespaceId = "namespaceId",
            status = Status("code", "comment"),
            version = Version(ImageTag(ImageRepository("", "", ""), "deployTag"), "auroraVersion", "releaseTo"),
            dockerImageRepo = "dockerImageRepo",
            time = Instant.EPOCH,
            applicationId = "appId"
        )
}

data class ApplicationDeploymentDetailsBuilder(val resourceLinks: List<Link> = emptyList()) {

    fun build() =
        ApplicationDeploymentDetailsResource(
            Instant.now(),
            GitInfoResource("123abc", Instant.now()),
            ImageDetailsResource(Instant.now(), "dockerImageReference"),
            listOf(
                PodResourceResource(
                    name = "name",
                    status = "status",
                    restartCount = 0,
                    ready = true,
                    startTime = Instant.now(),
                    managementResponses = ManagementResponsesResource(
                        HttpResponseResource(healthResponseJson, defaultInstant),
                        HttpResponseResource(infoResponseJson, defaultInstant)
                    )
                )
            ),
            ApplicationDeploymentCommandResource(
                emptyMap(),
                ApplicationDeploymentRefResource("environment", "application"),
                AuroraConfigRefResource("name", "refName")
            )
        ).apply {
            add(Link("http://ApplicationDeploymentDetails/1", "self"))
            add(resourceLinks)
        }
}

class ProbeResultListBuilder {
    fun build() = listOf(
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

class AuroraConfigFileBuilder {

    fun build() =
        AuroraConfigFileResource(
            name = "name",
            contents = "contents",
            type = AuroraConfigFileType.APP
        )
}
