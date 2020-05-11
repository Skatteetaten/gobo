package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusCheckResource
import no.skatteetaten.aurora.gobo.integration.skap.SkapJob
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag

data class StatusCheck(val name: String, val description: String, val failLevel: String, val hasFailed: Boolean)

data class Status(
    val code: String,
    val comment: String?,
    val reports: List<StatusCheck>,
    val reasons: List<StatusCheck>
)

data class Version(val deployTag: ImageTag, val auroraVersion: String?, val releaseTo: String?)

data class Route(
    val websealJobs: List<WebsealJob> = emptyList(),
    val bigipJobs: List<BigipJob> = emptyList()
)

data class WebsealJob(
    val id: String,
    val payload: String,
    val type: String,
    val operation: String,
    val status: String,
    val updated: String,
    val errorMessage: String? = null,
    val roles: List<String>? = null,
    val host: String? = null
) {

    companion object {

        fun create(skapJob: SkapJob): WebsealJob {
            val mapper = ObjectMapper()
/*
            val payload: JsonNode = mapper.readTree(skapJob.payload)
            val json: JsonObject = Parser().parse(jsonData) as JsonObject
*/

            val payload = mapper.readValue<Map<String, Any>>(skapJob.payload)
            val roles: List<String> by payload.withDefault { emptyList<String>() }
            val host: String by payload.withDefault { null }

            return WebsealJob(
                id = skapJob.id,
                payload = skapJob.payload,
                type = skapJob.type,
                operation = skapJob.operation,
                status = skapJob.status,
                updated = skapJob.updated,
                errorMessage = skapJob.errorMessage,
                roles = roles,
                host = host
            )
        }
    }
}

data class BigipJob(
    val id: String,
    val payload: String,
    val type: String,
    val operation: String,
    val status: String,
    val updated: String,
    val errorMessage: String? = null,
    val asmPolicy: String? = null,
    val externalHost: String? = null,
    val apiPaths: List<String>? = null,
    val oauthScopes: List<String>? = null,
    val hostname: String? = null,
    val serviceName: String? = null
) {

    companion object {

        fun create(skapJob: SkapJob): BigipJob {
            val mapper = ObjectMapper()
/*
            val payload: JsonNode = mapper.readTree(skapJob.payload)
            val json: JsonObject = Parser().parse(jsonData) as JsonObject
*/

            val payload = mapper.readValue<Map<String, Any>>(skapJob.payload)
            val asmPolicy: String by payload.withDefault { null }
            val externalHost: String by payload.withDefault { null }
            val apiPaths: List<String> by payload.withDefault { emptyList<String>() }
            val oauthScopes: List<String> by payload.withDefault { emptyList<String>() }
            val hostname: String by payload.withDefault { null }
            val serviceName: String by payload.withDefault { null }

            return BigipJob(
                id = skapJob.id,
                payload = skapJob.payload,
                type = skapJob.type,
                operation = skapJob.operation,
                status = skapJob.status,
                updated = skapJob.updated,
                errorMessage = skapJob.errorMessage,
                asmPolicy = asmPolicy,
                externalHost = externalHost,
                apiPaths = apiPaths,
                oauthScopes = oauthScopes,
                hostname = hostname,
                serviceName = serviceName
            )
        }
    }
}

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
    val message: String?,
    val imageRepository: ImageRepository?
) {
    companion object {
        fun create(deployment: ApplicationDeploymentResource) =
            create(deployment, deployment.dockerImageRepo?.let { ImageRepository.fromRepoString(it) })

        fun create(deployment: ApplicationDeploymentResource, imageRepo: ImageRepository?) =
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
                    ImageTag(deployment.dockerImageRepo?.let { ImageRepository.fromRepoString(it) }
                        ?: ImageRepository("", "", ""),
                        deployment.version.deployTag),
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
