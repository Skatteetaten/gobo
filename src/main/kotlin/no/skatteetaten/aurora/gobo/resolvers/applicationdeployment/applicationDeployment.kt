package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import java.time.Instant

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: ImageTag, val auroraVersion: String?, val releaseTo: String?)

data class ApplicationDeployment(
    val id: String,
    val name: String,
    val affiliationId: String,
    val environment: String,
    val namespaceId: String,
    val status: Status,
    val version: Version,
    val details: ApplicationDeploymentDetails?,
    val time: Instant
) {
    companion object {
        fun create(deployment: ApplicationDeploymentResource, details: ApplicationDeploymentDetails?) =
            ApplicationDeployment(
                id = deployment.identifier,
                name = deployment.name,
                affiliationId = deployment.affiliation,
                environment = deployment.environment,
                namespaceId = deployment.namespace,
                status = Status(deployment.status.code, deployment.status.comment),
                version = Version(
                    // TODO: This is far from ideal and manually adding ImageTag here should be considered a temporary
                    // adjustment. We need to move ImageTag out of version.
                    ImageTag(ImageRepository("", "", ""), deployment.version.deployTag),
                    deployment.version.auroraVersion,
                    deployment.version.releaseTo
                ),
                details = details,
                time = deployment.time
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

data class ApplicationDeploymentRefreshInput(val applicationDeploymentId: String)
data class ApplicationDeploymentVersionInput(val applicationDeploymentId: String, val version: String)
