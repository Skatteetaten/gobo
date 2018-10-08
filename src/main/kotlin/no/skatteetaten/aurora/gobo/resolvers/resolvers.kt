package no.skatteetaten.aurora.gobo.resolvers

import graphql.schema.DataFetchingEnvironment
import graphql.servlet.GraphQLContext
import org.dataloader.DataLoader
import org.dataloader.Try

val DataFetchingEnvironment.token: String?
    get() {
        val context = this.executionContext.context
        return when (context) {
            is GraphQLContext -> {
                val authorization = context.httpServletRequest
                    .map { it.getHeader("Authorization") }
                    .orElse(null)
                authorization?.split(" ")?.lastOrNull()?.trim()
            }
            else -> null
        }
    }

inline fun <reified T : Any> DataFetchingEnvironment.loader(): DataLoader<Any, Try<T>> {
    val key = T::class.java.simpleName
    val dataLoader = this.getContext<GraphQLContext>().dataLoaderRegistry.get()
        .getDataLoader<Any, Try<T>>(key) ?: throw IllegalStateException("No $key found")
    return dataLoader as? NoCacheBatchDataLoader ?: dataLoader as NoCacheBatchDataLoaderFlux
}