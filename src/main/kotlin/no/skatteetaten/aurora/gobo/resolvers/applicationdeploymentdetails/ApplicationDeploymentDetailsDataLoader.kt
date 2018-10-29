package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.EmptyResponseException
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoaderFlux
import no.skatteetaten.aurora.gobo.resolvers.handleError
import no.skatteetaten.aurora.gobo.resolvers.user.User
import no.skatteetaten.aurora.gobo.security.ANONYMOUS_USER
import org.dataloader.Try
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationService
) : KeysDataLoaderFlux<String, Try<ApplicationDeploymentDetails>> {

    override fun getByKeys(user: User, keys: List<String>): Flux<Try<ApplicationDeploymentDetails>> {
        // TODO will this keep ordering, because that is very important here
        return keys.toFlux().flatMap { key ->
            if (user == ANONYMOUS_USER) {
                failed(AccessDeniedException("Anonymous user cannot get details"))
            } else {
                applicationService.getApplicationDeploymentDetails(user.token, key)
                    .map { Try.succeeded(ApplicationDeploymentDetails.create(it)) }
                    .switchIfEmpty(failed(EmptyResponseException("User with name=$user did not get any response")))
                    .handleError()
                    .onErrorResume { failed(it) }
            }
        }
    }

    private fun failed(t: Throwable) = Try.failed<ApplicationDeploymentDetails>(t).toMono()
}