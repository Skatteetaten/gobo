package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import java.time.Instant

data class StatusCheck(val name: String, val description: String, val failLevel: String, val hasFailed: Boolean)

data class Status(
    val code: String,
    val comment: String?,
    val statusCheckName: String,
    val description: String,
    val details: List<StatusCheck>
)

data class Version(val deployTag: ImageTag, val auroraVersion: String?, val releaseTo: String?)

data class ApplicationDeployment(
    val id: String,
    val name: String,
    val affiliationId: String,
    val environment: String,
    val namespaceId: String,
    val status: Status,
    val version: Version,
    val dockerImageRepo: String?,
    val time: Instant,
    val applicationId: String
) {
    companion object {
        fun create(deployment: ApplicationDeploymentResource) =
            ApplicationDeployment(
                id = deployment.identifier,
                name = deployment.name,
                affiliationId = deployment.affiliation,
                environment = deployment.environment,
                namespaceId = deployment.namespace,
                status = Status(
                    deployment.status.code,
                    deployment.status.comment,
                    deployment.status.statusCheckName,
                    deployment.status.description,
                    deployment.status.details.map {
                        StatusCheck(
                            it.name,
                            it.description,
                            it.failLevel,
                            it.hasFailed
                        )
                    }),
                version = Version(
                    // TODO: This is far from ideal and manually adding ImageTag here should be considered a temporary
                    // adjustment. We need to move ImageTag out of version.
                    ImageTag(ImageRepository("", "", ""), deployment.version.deployTag),
                    deployment.version.auroraVersion,
                    deployment.version.releaseTo
                ),
                time = deployment.time,
                dockerImageRepo = deployment.dockerImageRepo,
                applicationId = deployment.applicationId
            )
    }
}

class ApplicationDeploymentBuilder {

    fun createApplicationDeployments(appResource: ApplicationResource): List<ApplicationDeployment> {
        return appResource.applicationDeployments.map { ApplicationDeployment.create(it) }
    }
}

data class RefreshByAffiliationsInput(val affiliations: List<String>)
data class RefreshByApplicationDeploymentIdInput(val applicationDeploymentId: String)
data class ApplicationDeploymentVersionInput(val applicationDeploymentId: String, val version: String)
