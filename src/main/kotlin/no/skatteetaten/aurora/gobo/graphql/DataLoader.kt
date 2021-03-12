package no.skatteetaten.aurora.gobo.graphql

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GraphQLExceptionWrapper
import java.util.concurrent.CompletableFuture

/**
 * Batches up the loaded keys and calls the data loader once with all keys.
 * Will return a list of values for each key.
 *
 * This function returns a CompletableFuture, must not be called from a suspended function.
 */
inline fun <Key, reified Value> DataFetchingEnvironment.loadBatchList(
    key: Key,
    loaderPrefix: String = Value::class.java.simpleName
): CompletableFuture<List<Value>> {
    val loaderName = "${loaderPrefix}BatchDataLoader"
    val loader = this.getDataLoader<Key, List<Value>>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")
    return loader.load(key, this.getContext())
}

/**
 * Batches up the loaded keys and calls the data loader once with all keys.
 * Will return one value for each key.
 *
 * This function returns a CompletableFuture, must not be called from a suspended function.
 */
inline fun <Key, reified Value> DataFetchingEnvironment.loadBatch(key: Key): CompletableFuture<Value> {
    val loaderName = "${Value::class.java.simpleName}BatchDataLoader"
    val loader = this.getDataLoader<Key, Value>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")
    return loader.load(key, this.getContext())
}

/**
 * Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader
 * If the loading throws an error the entire query will fail
 */
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
