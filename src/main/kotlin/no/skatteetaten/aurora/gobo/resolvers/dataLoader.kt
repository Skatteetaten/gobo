package no.skatteetaten.aurora.gobo.resolvers

import com.expediagroup.graphql.spring.exception.SimpleKotlinGraphQLError
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await

/**
 * Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader
 *
 * If the loading throws and error the entire query will fail
 */
suspend inline fun <Key, reified Value> DataFetchingEnvironment.load(
    key: Key,
    loaderPrefix: String = Value::class.java.simpleName
): Value {
    val loaderName = "${loaderPrefix}DataLoader"
    val loader = this.getDataLoader<Key, Value>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")
    return loader.load(key, this.getContext()).also {
        loader.dispatch()
    }.await()
}

/**
 * Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader
 * If the loading throws and error the entire query will fail
 */
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadMany(key: Key): List<Value> {
    val loaderName = "${Value::class.java.simpleName}ListDataLoader"
    val loader = this.getDataLoader<Key, List<Value>>(loaderName)
        ?: throw IllegalArgumentException("No data loader called $loaderName was found")
    return loader.load(key, this.getContext()).also {
        loader.dispatch()
    }.await()
}

/**
 * Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader
 * If the loading fails a partial result will be returned with data for successes and this failure in the error list
 */
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadOptional(key: Key): DataFetcherResult<Value?> {
    val dfr = DataFetcherResult.newResult<Value>()
    return try {
        dfr.data(load(key))
    } catch (e: Exception) {
        dfr.error(
            SimpleKotlinGraphQLError(
                e,
                listOf(mergedField.singleField.sourceLocation),
                path = executionStepInfo.path.toList()
            )
        )
    }.build()
}
