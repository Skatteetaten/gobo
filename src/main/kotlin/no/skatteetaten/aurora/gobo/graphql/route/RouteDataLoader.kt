package no.skatteetaten.aurora.gobo.graphql.route

import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import org.springframework.stereotype.Component

@Component
class RouteDataLoader(private val routeService: RouteService) : GoboDataLoader<ApplicationDeployment, DataFetcherResult<Route>>() {
    override suspend fun getByKeys(keys: Set<ApplicationDeployment>, ctx: GoboGraphQLContext): Map<ApplicationDeployment, DataFetcherResult<Route>> {
        return keys.associateWith {
            runCatching {
                val websealJobs = routeService.getSkapJobs(it.namespace.name, "${it.name}-webseal")
                    .map { skapJob -> WebsealJob.create(skapJob) }
                val bigipJobs = routeService.getSkapJobs(it.namespace.name, "${it.name}-bigip")
                    .map { skapJob -> BigipJob.create(skapJob) }
                newDataFetcherResult(Route(websealJobs = websealJobs, bigipJobs = bigipJobs))
            }.recoverCatching {
                newDataFetcherResult(it)
            }.getOrThrow()
        }
    }
}
