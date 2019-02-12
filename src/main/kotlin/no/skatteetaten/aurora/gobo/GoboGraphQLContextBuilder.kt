package no.skatteetaten.aurora.gobo

import graphql.servlet.GraphQLContext
import graphql.servlet.GraphQLContextBuilder
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.MultiKeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.batchDataLoaderMapped
import no.skatteetaten.aurora.gobo.resolvers.batchDataLoaderMappedSingle
import no.skatteetaten.aurora.gobo.security.ANONYMOUS_USER
import no.skatteetaten.aurora.gobo.security.currentUser
import org.dataloader.DataLoaderRegistry
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.websocket.Session
import javax.websocket.server.HandshakeRequest

@Component
class GoboGraphQLContextBuilder(
    val keyLoaders: List<KeyDataLoader<*, *>>,
    val multiKeyDataLoader: List<MultiKeyDataLoader<*, *>>
) : GraphQLContextBuilder {

    override fun build(httpServletRequest: HttpServletRequest?, httpServletResponse: HttpServletResponse?) =
        createContext(httpServletRequest)

    override fun build(session: Session?, handshakeRequest: HandshakeRequest?) = createContext()

    override fun build() = throw UnsupportedOperationException()

    private fun createContext(request: HttpServletRequest? = null): GraphQLContext {
        val currentUser = request?.currentUser() ?: ANONYMOUS_USER
        val registry = DataLoaderRegistry().apply {
            keyLoaders.forEach {
                register(it::class.simpleName, batchDataLoaderMappedSingle(currentUser, it))
            }
            multiKeyDataLoader.forEach {
                register(it::class.simpleName, batchDataLoaderMapped(currentUser, it))
            }
        }

        return GraphQLContext(request).apply {
            setDataLoaderRegistry(registry)
        }
    }
}