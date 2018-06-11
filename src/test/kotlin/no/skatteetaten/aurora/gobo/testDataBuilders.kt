package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.application.StatusResource
import no.skatteetaten.aurora.gobo.application.VersionResource
import no.skatteetaten.aurora.gobo.resolvers.application.Application
import no.skatteetaten.aurora.gobo.resolvers.application.Status
import no.skatteetaten.aurora.gobo.resolvers.application.Version

data class ApplicationResourceBuilder(
    val affiliation: String = "paas",
    val name: String = "name"
) {

    fun build(): ApplicationResource =
        ApplicationResource(
            affiliation,
            "environment",
            name,
            "namespace",
            StatusResource("code", "comment"),
            VersionResource("deployTag", "auroraVersion")
        )
}

data class ApplicationBuilder(
    val affiliationId: String = "paas",
    val name: String = "name"
) {

    fun build(): Application =
        Application(
            affiliationId,
            "environment",
            "namespaceId",
            name,
            Status("code", "comment"),
            Version("deployTag", "auroraVersion")
        )
}