package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusCheckResource
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import java.time.Instant

data class StatusCheck(val name: String, val description: String, val failLevel: String, val hasFailed: Boolean)

data class Status(
    val code: String,
    val comment: String?,
    val reports: List<StatusCheck>,
    val reasons: List<StatusCheck>
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
    val applicationId: String,
    val message: String?
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
                    code = deployment.status.code,
                    comment = "",
                    reports = deployment.status.reports.map(this::toStatusCheck),
                    reasons = deployment.status.reasons.map(this::toStatusCheck)
                ),
                version = Version(
                    // TODO: This is far from ideal and manually adding ImageTag here should be considered a temporary
                    // adjustment. We need to move ImageTag out of version.
                    ImageTag(ImageRepository("", "", ""), deployment.version.deployTag, ImageTagType.typeOf(deployment.version.deployTag)),
                    deployment.version.auroraVersion,
                    deployment.version.releaseTo


                ),
                time = deployment.time,
                dockerImageRepo = deployment.dockerImageRepo,
                applicationId = deployment.applicationId,
                message = deployment.message
            )

        private fun toStatusCheck(checkResource: StatusCheckResource) = checkResource.let {
            StatusCheck(
                name = it.name,
                description = it.description,
                failLevel = it.failLevel,
                hasFailed = it.hasFailed
            )
        }
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
data class ApplicationDeploymentIdInput(val applicationDeploymentId: String)