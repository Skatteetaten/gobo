package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoaderFlux
import no.skatteetaten.aurora.gobo.resolvers.user.User
import no.skatteetaten.aurora.gobo.security.UserService.Companion.ANONYMOUS_USER
import org.dataloader.Try
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux

@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationService
) : KeysDataLoaderFlux<String, Try<ApplicationDeploymentDetails>> {

    // TODO: If we get ReactiveSecurityContextToWork we could create a getByKey method that takes 1 key and retruns a Mono.
    // TODO: That mono is then transfered into a Try in getByKey
    override fun getByKeys(user: User, keys: List<String>): Flux<Try<ApplicationDeploymentDetails>> {
        // TODO will this keep ordering, because that is very important here
        return keys.toFlux().flatMap { key ->
            if (user == ANONYMOUS_USER) {
                Mono.just(
                    Try.failed<ApplicationDeploymentDetails>(
                        IllegalArgumentException("Anonymous user cannnot get details")
                    )
                )
            } else {
                applicationService.getApplicationDeploymentDetails(key, user.token)
                    .map { Try.succeeded(ApplicationDeploymentDetails.create(it)) }
                    .switchIfEmpty(
                        Mono.just(
                            Try.failed<ApplicationDeploymentDetails>(
                                IllegalArgumentException("User with name=$user did not get any response")
                            )
                        )
                    )
                    .onErrorResume { Mono.just(Try.failed<ApplicationDeploymentDetails>(it)) }
            }
        }
    }
}