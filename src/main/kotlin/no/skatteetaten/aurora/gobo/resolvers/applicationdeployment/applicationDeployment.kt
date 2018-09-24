package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: ImageTag, val auroraVersion: String?)

data class ApplicationDeployment(
    val id: String,
    val name: String,
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
                deployment.identifier,
                deployment.name,
                deployment.affiliation,
                deployment.environment,
                deployment.namespace,
                Status(deployment.status.code, deployment.status.comment),
                Version(
                    // TODO: This is far from ideal and manually adding ImageTag here should be considered a temporary
                    // adjustment. We need to move ImageTag out of version.
                    ImageTag(ImageRepository("", "", ""), deployment.version.deployTag),
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

data class ApplicationDeploymentVersionInput(val applicationDeploymentId: String, val version: String)
