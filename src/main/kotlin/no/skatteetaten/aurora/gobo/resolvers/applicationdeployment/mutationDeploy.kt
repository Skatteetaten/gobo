package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.ApplyPayload
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class DeployMutationResolver(
    private val applicationDeploymentService: ApplicationDeploymentService
) : GraphQLMutationResolver {

    fun deploy(input: ApplicationDeploymentInput, dfe: DataFetchingEnvironment): ApplicationDeploymentResult {

        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannotdeploy application")

        val payload = ApplyPayload(
            applicationDeploymentRefs = input.applicationDeployment.map {
                ApplicationDeploymentRefResource(it.environment, it.application)
            },
            overrides = input.overrides.associate { it.fileName to it.content }
        )

        val response =
            applicationDeploymentService.deploy(
                dfe.currentUser().token,
                input.auroraConfigName,
                input.auroraConfigReference,
                payload
            )

        val item = response.items.first()
        return ApplicationDeploymentResult(
            success = response.success,
            auroraConfigRef = AuroraConfigRef(
                name = item.auroraConfigRef.name,
                gitReference = item.auroraConfigRef.refName,
                commitId = item.auroraConfigRef.resolvedRef
            ),
            applicationDeployments = response.items.map {
                ApplicationDeploymentResultItem(
                    warnings = it.warnings,
                    tagResult = it.tagResponse,
                    openshiftResponses = it.openShiftResponses,
                    status = it.successString,
                    cluster = it.cluster,
                    environment = it.environment,
                    application = it.name,
                    version = it.version,
                    releaseTo = it.releaseTo,
                    deployId = it.deployId,
                    message = it.reason
                )
            }
        )
    }
}

data class ApplicationDeploymentInput(
    val applicationDeployment: List<ApplicationDeploymentRef>,
    val overrides: List<OverrideInput> = emptyList(),
    val auroraConfigName: String,
    val auroraConfigReference: String = "master"
)

data class ApplicationDeploymentRef(
    val environment: String,
    val application: String
)

data class OverrideInput(
    val fileName: String,
    val content: String
)

data class ApplicationDeploymentResult(
    val success: Boolean,
    val auroraConfigRef: AuroraConfigRef,
    val applicationDeployments: List<ApplicationDeploymentResultItem>
)

data class AuroraConfigRef(
    val name: String,
    val gitReference: String,
    val commitId: String
)

data class ApplicationDeploymentResultItem(
    val warnings: List<String>,
    val tagResult: JsonNode?,
    val openshiftResponses: List<JsonNode>,
    val status: String,
    val cluster: String,
    val environment: String,
    val application: String,
    val version: String,
    val releaseTo: String?,
    val deployId: String,
    val message: String?
)
