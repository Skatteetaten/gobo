package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusCheckResource
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.load
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import no.skatteetaten.aurora.gobo.resolvers.permission.Permission
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
    val affiliation: Affiliation,
    val environment: String,
    val namespace: Namespace,
    val status: Status,
    val version: Version,
    val dockerImageRepo: String?,
    val time: Instant,
    val applicationId: String,
    val message: String?,
    val imageRepository: ImageRepository?
) {

    suspend fun details(dfe: DataFetchingEnvironment) =
        dfe.load<String, ApplicationDeploymentDetails>(applicationId)

    companion object {
        fun create(deployment: ApplicationDeploymentResource) =
            create(deployment, deployment.dockerImageRepo?.let { ImageRepository.fromRepoString(it) })

        fun create(deployment: ApplicationDeploymentResource, imageRepo: ImageRepository?) =
            ApplicationDeployment(
                id = deployment.identifier,
                name = deployment.name,
                affiliation = Affiliation(deployment.affiliation),
                environment = deployment.environment,
                namespace = Namespace(deployment.namespace, Affiliation(deployment.affiliation), Permission()),
                status = Status(
                    code = deployment.status.code,
                    comment = "",
                    reports = deployment.status.reports.map(this::toStatusCheck),
                    reasons = deployment.status.reasons.map(this::toStatusCheck)
                ),
                version = Version(
                    // TODO: This is far from ideal and manually adding ImageTag here should be considered a temporary
                    // adjustment. We need to move ImageTag out of version.
                    ImageTag(
                        deployment.dockerImageRepo?.let { ImageRepository.fromRepoString(it) }
                            ?: ImageRepository("", "", ""),
                        deployment.version.deployTag
                    ),
                    deployment.version.auroraVersion,
                    deployment.version.releaseTo

                ),
                time = deployment.time,
                dockerImageRepo = deployment.dockerImageRepo,
                applicationId = deployment.applicationId,
                message = deployment.message,
                imageRepository = imageRepo
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

data class DeleteApplicationDeploymentInput(val namespace: String, val name: String)
