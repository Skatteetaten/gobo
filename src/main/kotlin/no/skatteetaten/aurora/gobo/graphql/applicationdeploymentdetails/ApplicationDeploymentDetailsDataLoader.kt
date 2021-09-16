package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationService
) : GoboDataLoader<String, ApplicationDeploymentDetails>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GoboGraphQLContext): Map<String, ApplicationDeploymentDetails> {
        return keys.associateWith { ApplicationDeploymentDetails.create(applicationService.getApplicationDeploymentDetails(ctx.token(), it)) }
    }
}
