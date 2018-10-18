package no.skatteetaten.aurora.gobo

import graphql.servlet.GraphQLContext
import graphql.servlet.GraphQLContextBuilder
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoaderFlux
import no.skatteetaten.aurora.gobo.resolvers.createNoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.createNoCacheBatchDataLoaderFlux
import no.skatteetaten.aurora.gobo.security.ANONYMOUS_USER
import no.skatteetaten.aurora.gobo.security.currentUser
import org.dataloader.DataLoaderRegistry
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.websocket.server.HandshakeRequest

@Component
class GoboGraphQLContextBuilder(
    val loaderList: List<KeysDataLoader<*, *>>,
    val loaderListFlux: List<KeysDataLoaderFlux<*, *>>
) : GraphQLContextBuilder {

    override fun build(httpServletRequest: HttpServletRequest?) = createContext(httpServletRequest)

    override fun build(handshakeRequest: HandshakeRequest?) = createContext()

    override fun build() = throw UnsupportedOperationException()

    private fun createContext(request: HttpServletRequest? = null): GraphQLContext {
        val currentUser = request?.currentUser() ?: ANONYMOUS_USER
        val registry = DataLoaderRegistry().apply {
            loaderList.forEach {
                register(it::class.simpleName, createNoCacheBatchDataLoader(currentUser, it))
            }
            loaderListFlux.forEach {
                register(it::class.simpleName, createNoCacheBatchDataLoaderFlux(currentUser, it))
            }
        }

        return GraphQLContext(request).apply {
            setDataLoaderRegistry(registry)
        }
    }
}