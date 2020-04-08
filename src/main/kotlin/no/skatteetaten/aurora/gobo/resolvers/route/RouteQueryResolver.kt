package no.skatteetaten.aurora.gobo.resolvers.route

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.Route
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
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL/BigIp progressions")
        return Route(progressions = routeService.getProgressions(namespace, name))
    }
}
