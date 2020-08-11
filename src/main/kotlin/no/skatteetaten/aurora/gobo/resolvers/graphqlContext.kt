package no.skatteetaten.aurora.gobo.resolvers

import com.expediagroup.graphql.execution.GraphQLContext
import com.expediagroup.graphql.spring.execution.GraphQLContextFactory
import graphql.schema.DataFetchingEnvironment
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException

fun DataFetchingEnvironment.token() = this.getContext<GoboGraphQLContext>().token
    ?: throw IllegalArgumentException("Token is not set")

class GoboGraphQLContext(val token: String?, val request: ServerHttpRequest, val response: ServerHttpResponse) :
    GraphQLContext

@Component
class GoboGraphQLContextFactory : GraphQLContextFactory<GoboGraphQLContext> {

    override suspend fun generateContext(request: ServerHttpRequest, response: ServerHttpResponse) = GoboGraphQLContext(
        token = request.headers.getFirst("Authorization")?.removePrefix("Bearer "),
        request = request,
        response = response
    )
}