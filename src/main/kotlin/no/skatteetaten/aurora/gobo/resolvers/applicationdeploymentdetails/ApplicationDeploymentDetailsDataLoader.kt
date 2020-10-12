package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationService
) : KeyDataLoader<String, ApplicationDeploymentDetails> {
    override suspend fun getByKey(key: String, context: GoboGraphQLContext): ApplicationDeploymentDetails {
        if (context.token == null) {
            throw AccessDeniedException("Anonymous user cannot get details")
        }

        return ApplicationDeploymentDetails.create(
            applicationService.getApplicationDeploymentDetails(
                context.token,
                key
            )
        )
    }
}
