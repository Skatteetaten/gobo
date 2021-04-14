package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentService
import no.skatteetaten.aurora.gobo.integration.boober.MultiAffiliationEnvironment
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

        val environments = names.flatMap { environmentService.getEnvironments(dfe.token(), it) }
        val applicationDeployments =
            applicationService.getApplicationDeployment(environments.flatMap { it.getApplicationDeploymentRefs() })

        // TODO hent status fra Phil

        return names.map { name ->
            val affiliations = applicationDeployments
                .filter { it.environment == name }
                .groupBy { it.affiliation }
                .map {
                    buildResources(
                        affiliation = it.key,
                        applicationDeployments = it.value,
                        environments = environments
                    )
                }

            // TODO filtrer på statusene fra Phil
            // 1) hvis status fra Phil er failed, bruk denne 2) hvis status fra Phil er success, bruk status fra ApplicationDeployment (Mokey)

            Environment(name, affiliations)
        }
    }

    private fun buildResources(
        affiliation: String,
        applicationDeployments: List<ApplicationDeploymentResource>,
        environments: List<MultiAffiliationEnvironment>
    ) = applicationDeployments.map { ad ->
        environments
            .filter { env -> env.affiliation == ad.affiliation }
            .flatMap { env -> env.deploymentRefs }
            .find { ref -> ref.environment == ad.environment && ref.application == ad.name }
            .let { deploymentRef -> EnvironmentApplication.create(ad, deploymentRef) }
    }.let { apps -> EnvironmentAffiliation(affiliation, apps) }
}
