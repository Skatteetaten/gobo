package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentResource
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class EnvironmentQuery(
    val applicationService: ApplicationService,
    val environmentService: EnvironmentService
) : Query {
    suspend fun environments(
        names: List<String>,
        dfe: DataFetchingEnvironment
    ): List<Environment> {
        dfe.checkValidUserToken()
        // TODO "upgrade" token, check group, use "super" token for Boober requests
        // 1. If Token not client or admin, refuse
        // 2. If only client, upgrade to gobo admin token for requests
        // 3. If admin, pass through

        val environments = names.flatMap { environmentService.getEnvironments(dfe.token(), it) }

        return names.map { envName ->
            environments
                .filter { it.containsEnvironment(envName) }
                .map { it.copy(deploymentRefs = it.deploymentRefs(envName)) }
                .map { it.createEnvironmentAffiliation() }
                .let { Environment(envName, it) }
        }
    }

    private fun EnvironmentResource.createEnvironmentAffiliation() = deploymentRefs.map { ref ->
        EnvironmentApplication(affiliation, ref)
    }.let {
        EnvironmentAffiliation(affiliation, it)
    }
}
