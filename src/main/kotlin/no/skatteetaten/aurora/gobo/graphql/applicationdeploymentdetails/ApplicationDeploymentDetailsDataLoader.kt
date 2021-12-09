package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationService
) : GoboDataLoader<String, ApplicationDeploymentDetails>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, ApplicationDeploymentDetails> {
        return keys.associateWith { ApplicationDeploymentDetails.create(applicationService.getApplicationDeploymentDetails(ctx.token, it)) }
    }
}
