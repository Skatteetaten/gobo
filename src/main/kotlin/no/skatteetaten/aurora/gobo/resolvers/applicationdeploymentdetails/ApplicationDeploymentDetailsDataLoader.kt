package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import kotlinx.coroutines.reactive.awaitFirst
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import org.springframework.stereotype.Component

/*
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
}*/

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
            ).awaitFirst()
        )
    }
}
