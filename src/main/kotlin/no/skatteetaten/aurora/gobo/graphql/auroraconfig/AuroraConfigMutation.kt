package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
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
        val result = service.updateAuroraConfigFile(
            dfe.token,
            input.auroraConfigName,
            input.auroraConfigReference ?: "master",
            input.fileName,
            input.contents,
            input.existingHash
        )
        return AuroraConfigFileValidationResponse(message = "OK", success = true, file = result)
    }

    suspend fun createAuroraConfigFile(
        input: NewAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        dfe.checkValidUserToken()
        val result = service.addAuroraConfigFile(
            dfe.token,
            input.auroraConfigName,
            input.auroraConfigReference ?: "master",
            input.fileName,
            input.contents
        )
        return AuroraConfigFileValidationResponse(message = "OK", success = true, file = result)
    }
}
