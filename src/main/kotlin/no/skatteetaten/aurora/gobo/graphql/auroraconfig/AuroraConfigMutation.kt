package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class AuroraConfigMutation(
    private val service: AuroraConfigService
) : Mutation {
    suspend fun updateAuroraConfigFile(
        input: UpdateAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        dfe.checkValidUserToken()
        return try {
            val result = service.updateAuroraConfigFile(
                dfe.token(),
                input.auroraConfigName,
                input.auroraConfigReference ?: "master",
                input.fileName,
                input.contents,
                input.existingHash
            )
            AuroraConfigFileValidationResponse(message = "OK", success = true, file = result)
        } catch (e: SourceSystemException) {
            AuroraConfigFileValidationResponse(
                message = e.message,
                success = false
            )
        }
    }

    suspend fun createAuroraConfigFile(
        input: NewAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        dfe.checkValidUserToken()
        return try {
            val result = service.addAuroraConfigFile(
                dfe.token(),
                input.auroraConfigName,
                input.auroraConfigReference ?: "master",
                input.fileName,
                input.contents
            )
            AuroraConfigFileValidationResponse(message = "OK", success = true, file = result)
        } catch (e: SourceSystemException) {
            AuroraConfigFileValidationResponse(
                message = e.message,
                success = false
            )
        }
    }
}
