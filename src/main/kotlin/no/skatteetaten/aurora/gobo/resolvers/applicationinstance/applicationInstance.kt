package no.skatteetaten.aurora.gobo.resolvers.applicationinstance

import no.skatteetaten.aurora.gobo.service.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.service.application.ApplicationInstanceResource
import no.skatteetaten.aurora.gobo.service.application.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails.ApplicationInstanceDetails

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class ApplicationInstance(
    val affiliationId: String,
    val environment: String,
    val namespaceId: String,
    val status: Status,
    val version: Version,
    val details: ApplicationInstanceDetails?
) {
    companion object {
        fun create(instance: ApplicationInstanceResource, details: ApplicationInstanceDetails?) =
            ApplicationInstance(
                instance.affiliation,
                instance.environment,
                instance.namespace,
                Status(instance.status.code, instance.status.comment),
                Version(
                    instance.version.deployTag,
                    instance.version.auroraVersion
                ),
                details
            )
    }
}

fun createApplicationInstances(
    resource: ApplicationResource,
    details: List<ApplicationInstanceDetailsResource>
): List<ApplicationInstance> {
    return resource.applicationInstances.map { instance ->
        val detailsResource =
            details.find { it.getLink("self")?.href == instance.getLink("ApplicationInstanceDetails")?.href }

        val applicationInstanceDetails = detailsResource?.let { ApplicationInstanceDetails.create(it) }
        ApplicationInstance.create(instance, applicationInstanceDetails)
    }
}
