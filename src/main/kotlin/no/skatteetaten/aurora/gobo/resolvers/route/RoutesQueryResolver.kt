package no.skatteetaten.aurora.gobo.resolvers.route

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.integration.skap.Routes
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class RoutesQueryResolver(
    val routeService: RouteService
) : GraphQLQueryResolver {

    fun routes(
        namespace: String,
        name: String,
        dfe: DataFetchingEnvironment
    ): Routes {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL/BipIp progressions")
        return Routes(progressions = routeService.getProgressions(namespace, name))
    }
}
