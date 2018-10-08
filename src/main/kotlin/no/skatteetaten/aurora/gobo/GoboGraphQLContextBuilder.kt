package no.skatteetaten.aurora.gobo

import graphql.servlet.GraphQLContext
import graphql.servlet.GraphQLContextBuilder
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoaderFlux
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoaderFlux
import org.dataloader.DataLoaderRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.websocket.server.HandshakeRequest

@Component
class GoboGraphQLContextBuilder(
    val loaderList: List<KeysDataLoader<*, *>>,
    val loaderListFlux: List<KeysDataLoaderFlux<*, *>>
) : GraphQLContextBuilder {

    private val logger: Logger = LoggerFactory.getLogger(GraphQLContextBuilder::class.java)

    override fun build(httpServletRequest: HttpServletRequest?) = createContext()

    override fun build(handshakeRequest: HandshakeRequest?) = createContext()

    override fun build() = createContext()

    private fun createContext(): GraphQLContext {
        logger.info("Creating new DataLoader instances")

        val registry = DataLoaderRegistry().apply {
            loaderList.forEach {
                register(it::class.simpleName, NoCacheBatchDataLoader(it))
            }
            loaderListFlux.forEach {
                register(it::class.simpleName, NoCacheBatchDataLoaderFlux(it))
            }
        }

        return GraphQLContext().apply {
            setDataLoaderRegistry(registry)
        }
    }
}