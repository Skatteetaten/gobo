package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationServiceBlocking
) : KeyDataLoader<String, ApplicationDeploymentDetails> {
    override fun getByKey(user: User, key: String): Try<ApplicationDeploymentDetails> {
        return if (user.name == "ANONYMOUS_USER.username") { // FIXME compare anonymous user
            Try.failed<ApplicationDeploymentDetails>(AccessDeniedException("Anonymous user cannot get details"))
        } else {
            Try.tryCall {
                applicationService.getApplicationDeploymentDetails(user.token, key).let {
                    ApplicationDeploymentDetails.create(it)
                }
            }
        }
    }
}
