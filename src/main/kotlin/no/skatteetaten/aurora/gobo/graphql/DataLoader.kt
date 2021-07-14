package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.exception.MissingDataLoaderException
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GraphQLExceptionWrapper
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * Load value(s) from the DataLoader for the key.
 * The function calling this must not be suspended and should return the CompletableFuture.
 */
inline fun <Key, reified Value> DataFetchingEnvironment.loadValue(
    key: Key,
    loaderClass: KClass<*>? = null
): CompletableFuture<Value> = getValueFromDataLoader(getLoaderName<Value>(loaderClass), key)

inline fun <Key, reified Value> DataFetchingEnvironment.loadValue(
    keys: List<Key>,
    loaderClass: KClass<*>? = null
): CompletableFuture<List<Value>> {
    val name = getLoaderName<Value>(loaderClass)
    val loader = getDataLoader<Key, Value>(name) ?: throw MissingDataLoaderException(name)
    return loader.loadMany(keys, listOf(getContext()))
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified Value> getLoaderName(loaderClass: KClass<*>?) =
    loaderClass?.let { loaderClass.simpleName }
        ?: when (Value::class) {
            // Gets the resource class name of the list type, such as List<DatabaseSchema>
            List::class -> typeOf<Value>().arguments.first().type.toString().substringAfterLast(".")
            // Returns the resource class name directly
            else -> Value::class.simpleName!!
        }.let {
            "${it}BatchDataLoader"
        }

/**
 * Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader
 * If the loading throws an error the entire query will fail
 */
@Deprecated(
    message = "Do not use this function of loading data from dataloader",
    replaceWith = ReplaceWith("loadValue or loadListValue")
)
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadOrThrow(
    key: Key,
    loaderPrefix: String = Value::class.java.simpleName
): Value {
    val loaderName = "${loaderPrefix}DataLoader"
    val loader = this.getDataLoader<Key, DataFetcherResult<Value>>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")
    return loader.load(key, this.getContext()).also {
        // When using nested data loaders, the second data loader is not called without dispatch
        // For more details https://github.com/graphql-java/graphql-java/issues/1198
        loader.dispatch()
    }.await().let { result ->
        result.exception()?.let { throw it } ?: result.data
    }
}

/**
 * Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader
 * If the loading throws an error the entire query will fail
 */
@Deprecated(
    message = "Do not use this function of loading data from dataloader",
    replaceWith = ReplaceWith("loadValue or loadListValue")
)
suspend inline fun <Key, reified Value> DataFetchingEnvironment.load(
    key: Key,
    loaderPrefix: String = Value::class.java.simpleName
): DataFetcherResult<Value?> {
    val loaderName = "${loaderPrefix}DataLoader"
    val loader = this.getDataLoader<Key, DataFetcherResult<Value?>>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")
    return loader.load(key, this.getContext()).also {
        // When using nested data loaders, the second data loader is not called without dispatch
        // For more details https://github.com/graphql-java/graphql-java/issues/1198
        loader.dispatch()
    }.await()
}

/**
 * Load a single key of type Key into a value of type Value using a dataloader named <Value>ListDataLoader
 * If the loading throws an error the entire query will fail
 */
@Deprecated(
    message = "Do not use this function of loading data from dataloader",
    replaceWith = ReplaceWith("loadValue or loadListValue")
)
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadMany(key: Key): List<Value> {
    val loaderName = "${Value::class.java.simpleName}ListDataLoader"
    val loader = this.getDataLoader<Key, DataFetcherResult<List<Value>>>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")
    return loader.load(key, this.getContext()).also {
        // When using nested data loaders, the second data loader is not called without dispatch
        // For more details https://github.com/graphql-java/graphql-java/issues/1198
        loader.dispatch()
    }.await().let { result ->
        result.exception()?.let { throw it } ?: result.data
    }
}

/**
 * Load multiple keys of type Key into a Map grouped by the key and a value of type Value using a dataloader named <Value>MultipleKeysDataLoader
 * If the loading fails it will return a failed DataFetcherResult
 */
@Deprecated(
    message = "Do not use this function of loading data from dataloader",
    replaceWith = ReplaceWith("loadValue or loadListValue")
)
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadMultipleKeys(keys: List<Key>): Map<Key, DataFetcherResult<Value>> {
    val loaderName = "${Value::class.java.simpleName}MultipleKeysDataLoader"
    val loader = this.getDataLoader<Key, DataFetcherResult<Value>>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")

    return keys.map {
        it to loader.load(it, this.getContext())
    }.toMap().also {
        // When using nested data loaders, the second data loader is not called without dispatch
        // For more details https://github.com/graphql-java/graphql-java/issues/1198
        loader.dispatch()
    }.mapValues {
        it.value.await()
    }
}

fun DataFetcherResult<*>.exception(): Throwable? = if (hasErrors()) {
    (errors.first() as GraphQLExceptionWrapper).exception
} else {
    null
}

fun <T> newDataFetcherResult(data: T, errors: List<Throwable> = emptyList()): DataFetcherResult<T> =
    DataFetcherResult.newResult<T>().data(data).errors(errors.map { GraphQLExceptionWrapper(it) }).build()
