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
}
