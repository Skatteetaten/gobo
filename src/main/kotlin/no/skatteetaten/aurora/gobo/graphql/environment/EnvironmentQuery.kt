package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentResource
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
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
        // TODO "upgrade" token, check group, use "super" token for Boober requests

        val environments = names.flatMap { environmentService.getEnvironments(dfe.token(), it) }

        // TODO get status from Phil
        return names.map { envName ->
            environments
                .filter { it.containsEnvironment(envName) }
                .map { it.copy(deploymentRefs = it.deploymentRefs(envName)) }
                .map {
                    GlobalScope.async(Dispatchers.IO) {
                        val refs = it.getApplicationDeploymentRefs(envName)
                        val applicationDeployments = applicationService.getApplicationDeployments(refs)
                        it.createEnvironmentAffiliation(envName, applicationDeployments)
                    }
                }
                .awaitAll()
                .let { Environment(envName, it) }
        }
    }

    private fun EnvironmentResource.createEnvironmentAffiliation(
        envName: String,
        applicationDeployments: List<ApplicationDeploymentResource>
    ) = deploymentRefs.map { ref ->
        val applicationDeployment = applicationDeployments.find(
            affiliation = affiliation,
            environment = envName,
            application = ref.application
        )

        // TODO filter of statuses from Phil
        // 1) If status from Phil is failed, use it
        // 2) If status from Phil is success, use status from ApplicationDeployment (Mokey)

        EnvironmentApplication.create(ref.application, applicationDeployment, ref)
    }.let {
        EnvironmentAffiliation(affiliation, it)
    }

    private fun List<ApplicationDeploymentResource>.find(
        affiliation: String,
        environment: String,
        application: String
    ) =
        this.find { ad ->
            ad.affiliation == affiliation && ad.environment == environment && ad.name == application
        }
}
