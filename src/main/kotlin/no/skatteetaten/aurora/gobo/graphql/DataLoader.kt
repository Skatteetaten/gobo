package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import com.expediagroup.graphql.server.extensions.getValuesFromDataLoader
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
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
): CompletableFuture<List<Value>> = getValuesFromDataLoader(getLoaderName<Value>(loaderClass), keys)

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified Value> getLoaderName(loaderClass: KClass<*>?) =
    loaderClass?.let { loaderClass.simpleName }
        ?: when (Value::class) {
            // Gets the resource class name of the List/DataFetcherResult type, such as List<DatabaseSchema>
            List::class, DataFetcherResult::class -> typeOf<Value>().arguments.first().type.toString()
                .substringAfterLast(".").removeSuffix("?")
            // Returns the resource class name directly
            else -> Value::class.simpleName!!
        }.let {
            "${it}DataLoader"
        }

fun <T> newDataFetcherResult(data: T, errors: List<Throwable> = emptyList()): DataFetcherResult<T> =
    DataFetcherResult.newResult<T>().data(data).errors(errors.map { GraphQLExceptionWrapper(it) }).build()

fun <T> newDataFetcherResult(vararg errors: Throwable): DataFetcherResult<T> =
    DataFetcherResult.newResult<T>().errors(errors.toList().map { GraphQLExceptionWrapper(it) }).build()
