package no.skatteetaten.aurora.gobo.graphql.route

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import org.springframework.stereotype.Component

@Component
class RouteDataLoader(private val routeService: RouteService) : KeyDataLoader<ApplicationDeployment, Route> {
    override suspend fun getByKey(key: ApplicationDeployment, context: GoboGraphQLContext): Route {
        println("HER?????")
        return Route(
            websealJobs = routeService.getSkapJobs(
                key.namespace.name,
                "${key.name}-webseal"
            ).map { WebsealJob.create(it) },
            bigipJobs = routeService.getSkapJobs(
                key.namespace.name,
                "${key.name}-bigip"
            ).map { BigipJob.create(it) }
        )
    }
}
