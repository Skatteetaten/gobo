package no.skatteetaten.aurora.gobo.graphql.route

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
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
        // TODO if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL/BigIp jobs")
        return Route(
            websealJobs = routeService.getSkapJobs(namespace, "$name-webseal").map { WebsealJob.create(it) },
            bigipJobs = routeService.getSkapJobs(namespace, "$name-bigip").map { BigipJob.create(it) }
        )
    }
}
