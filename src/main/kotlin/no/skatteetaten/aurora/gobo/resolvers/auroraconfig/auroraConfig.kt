package no.skatteetaten.aurora.gobo.resolvers.auroraapimetadata

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import org.springframework.stereotype.Component

@Component
class AuroraConfigQueryResolver(
    private val service: AuroraConfigService
) : GraphQLQueryResolver {

    fun auroraConfig(name: String, refInput: String?, dfe: DataFetchingEnvironment): AuroraConfig {
        val ref = refInput ?: "master"
        return AuroraConfig(name, ref, ref)
    }
}

@Component
class AuroraConfigResolver(
    private val service: AuroraConfigService
) : GraphQLResolver<AuroraConfig> {

    fun files(auroraConfig: AuroraConfig, dfe: DataFetchingEnvironment): List<AuroraConfigFileResource> {

        return emptyList()
    }
}

data class AuroraConfig(
    val name: String,
    val ref: String,
    val resolvedRef: String
)


