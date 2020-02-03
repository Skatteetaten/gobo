package no.skatteetaten.aurora.gobo.resolvers.auroraconfig

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class AuroraConfigQueryResolver(
    private val service: AuroraConfigService
) : GraphQLQueryResolver {

    fun auroraConfig(name: String, refInput: String?, dfe: DataFetchingEnvironment): AuroraConfig {
        val ref = refInput ?: "master"
        val token = dfe.currentUser().token
        return service.getAuroraConfig(token, name, ref)
    }
}

data class AuroraConfig(
    val name: String,
    val ref: String,
    val resolvedRef: String,
    val files: List<AuroraConfigFileResource>
)

@Component
class AuroraConfigResolve : GraphQLResolver<AuroraConfig> {

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

data class NewAuroraConfigFileInput(
    val auroraConfigName: String,
    val auroraConfigReference: String = "master",
    val fileName: String,
    val contents: String
)

data class UpdateAuroraConfigFileInput(
    val auroraConfigName: String,
    val auroraConfigReference: String = "master",
    val fileName: String,
    val contents: String,
    val existingHash: String
)

data class AuroraConfigFileValidationResponse(
    val message: String,
    val success: Boolean,
    val file: AuroraConfigFileResource?
)

data class ChangedAuroraConfigFileResponse(
    val errors: List<AuroraConfigValidationError> = emptyList(),
    val file: AuroraConfigFileResource? = null
)

data class AuroraConfigValidationError(
    val application: String,
    val environment: String,
    val details: List<AuroraConfigValidationErrorDetail>?,
    val type: String = "APPLICATION"
)

data class AuroraConfigValidationErrorDetail(
    val type: ErrorType,
    val message: String,
    val field: AuroraConfigFieldError? = null
)

enum class ErrorType {
    ILLEGAL,
    MISSING,
    INVALID,
    GENERIC,
    WARNING
}

data class AuroraConfigFieldError(
    val path: String,
    val fileName: String? = null,
    val value: String? = null
)
