package no.skatteetaten.aurora.gobo

import graphql.servlet.GraphQLContext
import graphql.servlet.GraphQLContextBuilder
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoaderFlux
import no.skatteetaten.aurora.gobo.resolvers.createNoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.createNoCacheBatchDataLoaderFlux
import no.skatteetaten.aurora.gobo.security.UserService
import org.dataloader.DataLoaderRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.websocket.server.HandshakeRequest

@Component
class GoboGraphQLContextBuilder(
    val userService: UserService,
    val loaderList: List<KeysDataLoader<*, *>>,
    val loaderListFlux: List<KeysDataLoaderFlux<*, *>>
) : GraphQLContextBuilder {

    private val logger: Logger = LoggerFactory.getLogger(GraphQLContextBuilder::class.java)

    override fun build(httpServletRequest: HttpServletRequest?) = createContext(httpServletRequest)

    override fun build(handshakeRequest: HandshakeRequest?) = createContext()

    override fun build() = throw UnsupportedOperationException()

    private fun createContext(httpServletRequest: HttpServletRequest? = null): GraphQLContext {
        logger.info("Creating new DataLoader instances")

        val currentUser = userService.getCurrentUser(httpServletRequest)
        val registry = DataLoaderRegistry().apply {
            loaderList.forEach {
                register(it::class.simpleName, createNoCacheBatchDataLoader(currentUser, it))
            }
            loaderListFlux.forEach {
                register(it::class.simpleName, createNoCacheBatchDataLoaderFlux(currentUser, it))
            }
        }

        return GraphQLContext(httpServletRequest).apply {
            setDataLoaderRegistry(registry)
        }
    }
}