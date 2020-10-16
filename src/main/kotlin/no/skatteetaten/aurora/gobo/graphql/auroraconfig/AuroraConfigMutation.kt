package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import org.springframework.stereotype.Component

@Component
class AuroraConfigMutation(
    private val service: AuroraConfigService
) : Mutation {
    // FIXME no anonymous access
    suspend fun updateAuroraConfigFile(
        input: UpdateAuroraConfigFileInput,
        dfe: DataFetchingEnvironment
    ): AuroraConfigFileValidationResponse {
        val token = dfe.token()
        val addResult =
            service.updateAuroraConfigFile(
                token,
                input.auroraConfigName,
                input.auroraConfigReference ?: "master",
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
            )

        val result: AuroraConfigFileResource = addResult.items.first()

        return AuroraConfigFileValidationResponse(
            message = addResult.message,
            success = addResult.success,
            file = result
        )
    }
}
