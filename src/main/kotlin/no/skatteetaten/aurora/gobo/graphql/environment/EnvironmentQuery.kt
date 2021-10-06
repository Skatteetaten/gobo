package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentResource
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import no.skatteetaten.aurora.gobo.security.getValidUser
import no.skatteetaten.aurora.springboot.OpenShiftTokenIssuer
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

const val adminGroup = "APP_kryssaffiliation_admin_utv"
const val clientGroup = "APP_kryssaffiliation_client_utv"

@Component
class EnvironmentQuery(
    val applicationService: ApplicationService,
    val environmentService: EnvironmentService,
    val openShiftTokenIssuer: OpenShiftTokenIssuer,
) : Query {
    suspend fun environments(
        names: List<String>,
        dfe: DataFetchingEnvironment
    ): List<Environment> {
        dfe.checkValidUserToken()

        val user = dfe.getValidUser()
        val token = when {
            user.groups.admin() -> user.token
            user.groups.client() -> openShiftTokenIssuer.getToken()
            else -> throw ResponseStatusException(UNAUTHORIZED)
        }
        val environments = names.flatMap { environmentService.getEnvironments(token, it) }

        return names.map { envName ->
            environments
                .filter { it.containsEnvironment(envName) }
                .map { it.copy(deploymentRefs = it.deploymentRefs(envName)) }
                .map { it.createEnvironmentAffiliation() }
                .let { Environment(envName, it) }
        }
    }

    private fun List<String>.client(): Boolean = any { it == clientGroup }
    private fun List<String>.admin(): Boolean = any { it == adminGroup }

    private fun EnvironmentResource.createEnvironmentAffiliation() = deploymentRefs.map { ref ->
        EnvironmentApplication(affiliation, ref)
    }.let {
        EnvironmentAffiliation(affiliation, it)
    }
}
