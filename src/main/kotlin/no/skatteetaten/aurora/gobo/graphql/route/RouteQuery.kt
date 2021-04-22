package no.skatteetaten.aurora.gobo.graphql.route

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class RouteQuery(
    val routeService: RouteService
) : Query {

    suspend fun route(
        namespace: String,
        name: String,
        dfe: DataFetchingEnvironment
    ): Route {
        dfe.checkValidUserToken()
        return Route(
            websealJobs = routeService.getSkapJobs(namespace, "$name-webseal").map { WebsealJob.create(it) },
            bigipJobs = routeService.getSkapJobs(namespace, "$name-bigip").map { BigipJob.create(it) }
        )
    }
}
