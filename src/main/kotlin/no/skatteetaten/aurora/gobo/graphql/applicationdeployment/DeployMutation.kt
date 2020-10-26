package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.expediagroup.graphql.spring.operations.Mutation
import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.ApplyPayload
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.ApplicationDeploymentSpec
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.security.checkValidToken
import org.springframework.stereotype.Component

@Component
class DeployMutation(
    private val applicationDeploymentService: ApplicationDeploymentService
) : Mutation {

    // FIXME do not allow anonymous access
    suspend fun deploy(input: DeployApplicationDeploymentInput, dfe: DataFetchingEnvironment): ApplicationDeploymentResult {
        dfe.checkValidToken()

        val payload = ApplyPayload(
            applicationDeploymentRefs = input.applicationDeployment.map {
                ApplicationDeploymentRefResource(it.environment, it.application)
            },
            overrides = input.overrides?.associate { it.fileName to it.content } ?: emptyMap()
        )

        val response =
            applicationDeploymentService.deploy(
                dfe.token(),
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
                    spec = ApplicationDeploymentSpec(it.deploymentSpec),
                    deployId = it.deployId,
                    message = it.reason,
                    applicationDeploymentId = it.applicationDeploymentId
                )
            }
        )
    }
}

// FIXME need to rename this due to naming collision with ApplicationDeployment input
data class DeployApplicationDeploymentInput(
    val applicationDeployment: List<ApplicationDeploymentRef>,
    val overrides: List<OverrideInput>? = emptyList(),
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
    val applicationDeploymentId: String,
    val tagResult: JsonNode?,
    val openshiftResponses: List<JsonNode>,
    val status: String,
    val spec: ApplicationDeploymentSpec,
    val deployId: String,
    val message: String?
)
