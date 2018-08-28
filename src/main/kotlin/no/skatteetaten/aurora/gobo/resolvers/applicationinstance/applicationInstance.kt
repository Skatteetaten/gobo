package no.skatteetaten.aurora.gobo.resolvers.applicationinstance

import no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails.ApplicationInstanceDetails
import no.skatteetaten.aurora.gobo.service.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.service.application.ApplicationInstanceResource
import no.skatteetaten.aurora.gobo.service.application.ApplicationResource

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

// TODO: Provide better error messages for the double bangs
class ApplicationInstanceBuilder(detailsResources: List<ApplicationInstanceDetailsResource>) {

    private val detailsIndex = detailsResources
        .map { Pair(it.getLink("self")!!.href, it) }
        .toMap()

    fun createApplicationInstances(appResource: ApplicationResource): List<ApplicationInstance> {

        return appResource.applicationInstances.map { instance ->
            val detailsLink = instance.getLink("ApplicationInstanceDetails")?.href!!
            val detailsResource = detailsIndex[detailsLink]!!
            val applicationInstanceDetails = ApplicationInstanceDetails.create(detailsResource)
            ApplicationInstance.create(instance, applicationInstanceDetails)
        }
    }
}
