package no.skatteetaten.aurora.gobo.resolvers.auroraconfig

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.reactive.awaitFirst
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.resolvers.loadMany
import no.skatteetaten.aurora.gobo.resolvers.token
import org.springframework.stereotype.Component

@Component
class AuroraConfigQuery(
    private val service: AuroraConfigService
) : Query {

    suspend fun auroraConfig(name: String, refInput: String?, dfe: DataFetchingEnvironment): AuroraConfig {
        val ref = refInput ?: "master"
        val token = dfe.token()
        return service.getAuroraConfig(token, name, ref)
    }
}

@Component
class AuroraConfigMuration(
    private val service: AuroraConfigService
) : Mutation {
    // FIXME no anonymous access
    suspend fun updateAuroraConfigFile(
        input: UpdateAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        val token = dfe.token()
        val addResult: Response<AuroraConfigFileResource> =
            service.updateAuroraConfigFile(
                token,
                input.auroraConfigName,
                input.auroraConfigReference ?: "master",
                input.fileName,
                input.contents,
                input.existingHash
            ).awaitFirst()

        val result: AuroraConfigFileResource = addResult.items.first()

        return AuroraConfigFileValidationResponse(
            message = addResult.message,
            success = addResult.success,
            file = result
        )
    }

    // FIXME no anonymous user access
    suspend fun createAuroraConfigFile(
        input: NewAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        val token = dfe.token()
        val addResult: Response<AuroraConfigFileResource> =
            service.addAuroraConfigFile(
                token,
                input.auroraConfigName,
                input.auroraConfigReference,
                input.fileName,
                input.contents
            ).awaitFirst()

        val result: AuroraConfigFileResource = addResult.items.first()

        return AuroraConfigFileValidationResponse(
            message = addResult.message,
            success = addResult.success,
            file = result
        )
    }
}

data class AuroraConfig(
    val name: String,
    val ref: String,
    val resolvedRef: String,
    val files: List<AuroraConfigFileResource>
) {

    // FIXME no anonymous access
    fun files(
        types: List<AuroraConfigFileType>?,
        fileNames: List<String>?
    ): List<AuroraConfigFileResource> {
        return files.filter {
            types == null || types.contains(it.type)
        }.filter {
            fileNames == null || fileNames.contains(it.name)
        }
    }

    // FIXME no anonymous access
    suspend fun applicationDeploymentSpec(
        applicationDeploymentRefs: List<ApplicationDeploymentRef>,
        dfe: DataFetchingEnvironment
    ): List<ApplicationDeploymentSpec> {
        return dfe.loadMany(
            AdSpecKey(
                name,
                ref,
                applicationDeploymentRefs
            )
        )
    }
}

data class ApplicationDeploymentSpec(
    val rawJsonValueWithDefaults: JsonNode
) {
    val cluster = rawJsonValueWithDefaults.at("/cluster/value").textValue()
    val environment = rawJsonValueWithDefaults.at("/envName/value").textValue()
    val name = rawJsonValueWithDefaults.at("/name/value").textValue()
    val version = rawJsonValueWithDefaults.at("/version/value").textValue()
    val releaseTo: String? = rawJsonValueWithDefaults.at("/releaseTo/value").textValue()
    val application = rawJsonValueWithDefaults.at("/name/value").textValue()
    val type = rawJsonValueWithDefaults.at("/type/value").textValue()
    val deployStrategy = rawJsonValueWithDefaults.at("/deployStrategy/type/value").textValue()
    val replicas = rawJsonValueWithDefaults.at("/replicas/value").intValue().toString()
    val paused = rawJsonValueWithDefaults.at("/pause/value").booleanValue()
}

data class AuroraConfigFileResource(
    val name: String,
    val contents: String,
    val type: AuroraConfigFileType,
    val contentHash: String
)

/*
@Component
class AuroraConfigResolver(val applicationDeploymentService: ApplicationDeploymentService) :
    GraphQLResolver<AuroraConfig> {

    fun applicationDeploymentSpec(
        auroraConfig: AuroraConfig,
        appliationDeploymentRefs: List<ApplicationDeploymentRef>,
        dfe: DataFetchingEnvironment
    ): List<ApplicationDeploymentSpec> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get aurora config")
        val token = dfe.currentUser().token
        return applicationDeploymentService.getSpec(
            token,
            auroraConfig.name,
            auroraConfig.ref,
            appliationDeploymentRefs
        )
    }

    fun files(
        auroraConfig: AuroraConfig,
        types: List<AuroraConfigFileType>?,
        fileNames: List<String>?,
        dfe: DataFetchingEnvironment
    ): List<AuroraConfigFileResource> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get aurora config")

        return auroraConfig.files.filter {
            types == null || types.contains(it.type)
        }.filter {
            fileNames == null || fileNames.contains(it.name)
        }
    }
}

@Component
class AuroraConfigMutationResolver(
    private val service: AuroraConfigService
) : GraphQLMutationResolver {

    fun updateAuroraConfigFile(
        input: UpdateAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot create aurora config file")
        val token = dfe.currentUser().token
        val addResult: Response<AuroraConfigFileResource> =
            service.updateAuroraConfigFile(
                token,
                input.auroraConfigName,
                input.auroraConfigReference,
                input.fileName,
                input.contents,
                input.existingHash
            )

        val result: AuroraConfigFileResource = addResult.items.first()

        return AuroraConfigFileValidationResponse(
            message = addResult.message,
            success = addResult.success,
            file = result
        )
    }

    fun createAuroraConfigFile(
        input: NewAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot create aurora config file")
        val token = dfe.currentUser().token
        val addResult: Response<AuroraConfigFileResource> =
            service.addAuroraConfigFile(
                token,
                input.auroraConfigName,
                input.auroraConfigReference,
                input.fileName,
                input.contents
            )

        val result: AuroraConfigFileResource = addResult.items.first()

        return AuroraConfigFileValidationResponse(
            message = addResult.message,
            success = addResult.success,
            file = result
        )
    }
}
*/

data class NewAuroraConfigFileInput(
    val auroraConfigName: String,
    val auroraConfigReference: String = "master",
    val fileName: String,
    val contents: String
)

data class UpdateAuroraConfigFileInput(
    val auroraConfigName: String,
    val auroraConfigReference: String?,
    val fileName: String,
    val contents: String,
    val existingHash: String
)

data class AuroraConfigFileValidationResponse(
    val message: String,
    val success: Boolean,
    val file: AuroraConfigFileResource?
)
