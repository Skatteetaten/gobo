package no.skatteetaten.aurora.gobo.resolvers

import graphql.schema.DataFetchingEnvironment
import graphql.servlet.GraphQLContext
import org.dataloader.DataLoader
import org.dataloader.Try
import kotlin.reflect.KClass

fun <T : Any> DataFetchingEnvironment.loader(type: KClass<T>): DataLoader<Any, Try<T>> {
    val key = "${type.simpleName}DataLoader"
    return this.getContext<GraphQLContext>().dataLoaderRegistry.get()
        .getDataLoader<Any, Try<T>>(key) ?: throw IllegalStateException("No $key found")
}