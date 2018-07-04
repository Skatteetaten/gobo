package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.application.ApplicationInstanceResource
import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.application.GitInfoResource
import no.skatteetaten.aurora.gobo.application.ImageDetailsResource
import no.skatteetaten.aurora.gobo.application.PodResourceResource
import no.skatteetaten.aurora.gobo.application.StatusResource
import no.skatteetaten.aurora.gobo.application.VersionResource
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.ApplicationInstance
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.Status
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.Version
import java.time.Instant

data class ApplicationInstanceResourceBuilder(val affiliation: String = "paas") {
    fun build(): ApplicationInstanceResource =
        ApplicationInstanceResource(
            affiliation,
            "environment",
            "namespace",
            StatusResource("code", "comment"),
            VersionResource("deployTag", "auroraVersion")
        )
}

data class ApplicationResourceBuilder(val name: String = "name") {

    fun build(): ApplicationResource =
        ApplicationResource(
            name,
            emptyList(),
            listOf(ApplicationInstanceResourceBuilder().build())
        )
}

data class ApplicationInstanceBuilder(val affiliation: String = "paas") {

    fun build(): ApplicationInstance =
        ApplicationInstance(
            affiliation,
            "environment",
            "namespaceId",
            Status("code", "comment"),
            Version("deployTag", "auroraVersion"),
            null
        )
}

class ApplicationInstanceDetailsBuilder {

    fun build(): ApplicationInstanceDetailsResource =
        ApplicationInstanceDetailsResource(
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
        )
}
