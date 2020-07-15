package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.MyGraphQLContext
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationServiceBlocking
) : KeyDataLoader<String, ApplicationDeploymentDetails> {

    override suspend fun getByKey(key: String, ctx: MyGraphQLContext): ApplicationDeploymentDetails {
        return applicationService.getApplicationDeploymentDetails("token", key).let {
            ApplicationDeploymentDetails.create(it)
        }
    }
}
