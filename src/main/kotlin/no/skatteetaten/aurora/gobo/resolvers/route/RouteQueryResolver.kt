package no.skatteetaten.aurora.gobo.resolvers.route

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class RouteQueryResolver(
    val routeService: RouteService
) : GraphQLQueryResolver {

    fun route(
        namespace: String,
        name: String,
        dfe: DataFetchingEnvironment
    ): Route {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL/BigIp jobs")
        return Route(
            websealJobs = routeService.getSkapJobs(namespace, "$name-webseal").map { WebsealJob.create(it) },
            bigipJobs = routeService.getSkapJobs(namespace, "$name-bigip").map { BigipJob.create(it) }
        )
    }
}
