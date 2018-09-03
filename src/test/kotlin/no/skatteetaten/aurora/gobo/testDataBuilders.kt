package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.Status
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.Version
import no.skatteetaten.aurora.gobo.service.application.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.service.application.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.service.application.ApplicationResource
import no.skatteetaten.aurora.gobo.service.application.GitInfoResource
import no.skatteetaten.aurora.gobo.service.application.ImageDetailsResource
import no.skatteetaten.aurora.gobo.service.application.PodResourceResource
import no.skatteetaten.aurora.gobo.service.application.StatusResource
import no.skatteetaten.aurora.gobo.service.application.VersionResource
import org.springframework.hateoas.Link
import java.time.Instant

data class ApplicationDeploymentResourceBuilder(val affiliation: String = "paas") {
    fun build(): ApplicationDeploymentResource =
        ApplicationDeploymentResource(
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
            name,
            listOf(ApplicationDeploymentResourceBuilder().build())
        )
}

data class ApplicationDeploymentBuilder(val affiliation: String = "paas") {

    fun build(): ApplicationDeployment =
        ApplicationDeployment(
            affiliation,
            "environment",
            "namespaceId",
            Status("code", "comment"),
            Version("deployTag", "auroraVersion"),
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
            )
        ).apply { add(Link("http://ApplicationDeploymentDetails/1", "self")) }
}
