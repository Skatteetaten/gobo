package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationService
) : KeyDataLoader<String, ApplicationDeploymentDetails> {
    override suspend fun getByKey(key: String, context: GoboGraphQLContext): ApplicationDeploymentDetails {
        return ApplicationDeploymentDetails.create(
            applicationService.getApplicationDeploymentDetails(
                context.token(),
                key
            )
        )
    }
}
