package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import org.springframework.stereotype.Component

@Component
class AuroraConfigQuery(
    private val service: AuroraConfigService
) : Query {

    suspend fun auroraConfig(name: String, refInput: String? = null, dfe: DataFetchingEnvironment) =
        service.getAuroraConfig(dfe.token(), name, refInput ?: "master")

    suspend fun applicationAuroraConfigFiles(
        name: String,
        environment: String,
        application: String,
        dfe: DataFetchingEnvironment
    ) =
        service.getApplicationAuroraConfigFiles(dfe.token(), name, environment, application)
}
