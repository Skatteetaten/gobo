package no.skatteetaten.aurora.gobo.resolvers.auroraconfig

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.security.currentUser
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

    fun createAuroraConfigFile(
        input: NewAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        val ref = input.auroraConfigReference ?: "master"
        val token = dfe.currentUser().token
        val addResult: Response<ApplicationError> =
            service.addAuroraConfigFile(token, input.auroraConfigName, ref, input.fileName, input.contents)

        val result = AuroraConfigFileValidationResponse(
            message = addResult.message,
            success = addResult.success,
            errors = addResult.items
        )

        return result
    }
}

data class NewAuroraConfigFileInput(
    val auroraConfigName: String,
    val auroraConfigReference: String?,
    val fileName: String,
    val contents: String
)

data class AuroraConfigFileValidationResponse(
    val message: String,
    val success: Boolean,
    val errors: List<ApplicationError>?
)

data class ApplicationError(
    val application: String,
    val environment: String,
    val details: List<ErrorDetail>?,
    val type: String = "APPLICATION"
)

data class ErrorDetail(
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
