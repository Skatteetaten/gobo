package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class EnvironmentQuery(
    val applicationService: ApplicationService,
    val environmentService: EnvironmentService
) : Query {

    suspend fun environments(names: List<String>, dfe: DataFetchingEnvironment): List<Environment> {
        dfe.checkValidUserToken()

        val environments = names.flatMap { environmentService.getEnvironments(dfe.token(), it) }
        val applicationDeployments =
            applicationService.getApplicationDeployment(environments.flatMap { it.applicationDeploymentRefs })

        // TODO hent status fra Phil

        return names.map { env ->
            val affiliations = applicationDeployments
                .filter { it.environment == env }
                .groupBy { it.affiliation }
                .map {
                    val apps = it.value.map { ad -> EnvironmentApplication.create(ad) }
                    EnvironmentAffiliation(it.key, apps)
                }

            Environment(env, affiliations)
        }
    }
}
