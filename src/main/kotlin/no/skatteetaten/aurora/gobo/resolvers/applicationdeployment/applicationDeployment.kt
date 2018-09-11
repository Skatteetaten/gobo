package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class ApplicationDeployment(
    val affiliationId: String,
    val environment: String,
    val namespaceId: String,
    val status: Status,
    val version: Version,
    val details: ApplicationDeploymentDetails?
) {
    companion object {
        fun create(deployment: ApplicationDeploymentResource, details: ApplicationDeploymentDetails?) =
            ApplicationDeployment(
                deployment.affiliation,
                deployment.environment,
                deployment.namespace,
                Status(deployment.status.code, deployment.status.comment),
                Version(
                    deployment.version.deployTag,
                    deployment.version.auroraVersion
                ),
                details
            )
    }
}

// TODO: Provide better error messages for the double bangs
class ApplicationDeploymentBuilder(deploymentResources: List<ApplicationDeploymentDetailsResource>) {

    private val detailsIndex = deploymentResources
        .map { Pair(it.getLink("self")!!.href, it) }
        .toMap()

    fun createApplicationDeployments(appResource: ApplicationResource): List<ApplicationDeployment> {

        return appResource.applicationDeployments.map { deployment ->
            val detailsLink = deployment.getLink("ApplicationDeploymentDetails")?.href!!
            val detailsResource = detailsIndex[detailsLink]!!
            val applicationDeploymentDetails = ApplicationDeploymentDetails.create(detailsResource)
            ApplicationDeployment.create(deployment, applicationDeploymentDetails)
        }
    }
}
