package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.spring.exception.SimpleKotlinGraphQLError
import com.expediagroup.graphql.spring.execution.DataLoaderRegistryFactory
import com.expediagroup.graphql.spring.execution.GraphQLContextFactory
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.dataloader.Try
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component

class MyGraphQLContext(val user: String?, val request: ServerHttpRequest, val response: ServerHttpResponse)

@Component
class MyGraphQLContextFactory: GraphQLContextFactory<MyGraphQLContext> {

    override suspend fun generateContext(request: ServerHttpRequest, response: ServerHttpResponse): MyGraphQLContext = MyGraphQLContext(
        user = request.headers.getFirst("Authorization")?.removePrefix("Bearer "),
        request = request,
        response = response)
}

@Configuration
class DataLoaderConfiguration(
    val keyLoaders: List<KeyDataLoader<*, *>>,
    val multipleKeysDataLoaders: List<MultipleKeysDataLoader<*, *>>
) {
    @Bean
    fun dataLoaderRegistryFactory(): DataLoaderRegistryFactory {
        return object : DataLoaderRegistryFactory {

            override fun generate(): DataLoaderRegistry {
                return DataLoaderRegistry().apply {
                    keyLoaders.forEach {
                        register(it::class.simpleName, batchDataLoaderMappedSingle(it))
                    }
                    multipleKeysDataLoaders.forEach {
                        register(it::class.simpleName, batchDataLoaderMappedMultiple(it))
                    }
                }
            }
        }
    }
}


interface KeyDataLoader<K, V> {
    suspend fun getByKey(key: K, ctx: MyGraphQLContext): V
}

interface MultipleKeysDataLoader<K, V> {
    suspend fun getByKeys(keys: Set<K>, ctx: MyGraphQLContext): Map<K, Try<V>>
}

/*
  Use this if you have a service that loads multiple ids.
 */
fun <K, V> batchDataLoaderMappedMultiple(keysDataLoader: MultipleKeysDataLoader<K, V>): DataLoader<K, V> =
    DataLoader.newMappedDataLoaderWithTry{ keys: Set<K>, env: BatchLoaderEnvironment ->
        GlobalScope.async {
            keysDataLoader.getByKeys(keys, env.keyContexts.entries.first().value as MyGraphQLContext)
        }.asCompletableFuture()
    }


/*
  Use this if you have a service that loads a single id. Will make requests in parallel
 */
fun <K, V> batchDataLoaderMappedSingle(keyDataLoader: KeyDataLoader<K, V>): DataLoader<K, V> =
    DataLoader.newMappedDataLoaderWithTry { keys: Set<K>, env:BatchLoaderEnvironment ->

        GlobalScope.async {
            val deferred: List<Deferred<Pair<K, Try<V>>>> = keys.map { key ->
                async {
                    key to try {
                        val ctx = env.keyContexts[key] as MyGraphQLContext
                        Try.succeeded(keyDataLoader.getByKey(key, ctx))
                    } catch (e: Exception) {
                        Try.failed<V>(e)
                    }
                }
            }
            deferred.awaitAll().toMap()
        }.asCompletableFuture()
    }

/*
  Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader

  If the loading throws and error the entire query will fail
 */
suspend inline fun <Key, reified Value> DataFetchingEnvironment.load(key: Key, loaderPrefix: String = Value::class.java.simpleName): Value {
    val loaderName = "${loaderPrefix}DataLoader"
    return this.getDataLoader<Key, Value>(loaderName).load(key, this.getContext()).await()
}

/*
  Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader

  If the loading throws and error the entire query will fail
 */
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadMany(key: Key): List<Value> {
    val loaderName = "${Value::class.java.simpleName}ListDataLoader"
    return this.getDataLoader<Key, List<Value>>(loaderName).load(key, this.getContext()).await()
}

/*
  Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader

  If the loading fails a partial result will be returned with data for successes and this failure in the error list
 */
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadOptional(key: Key): DataFetcherResult<Value?> {

    val dfr = DataFetcherResult.newResult<Value>()
    return try {
        dfr.data(load(key))
    }catch(e:Exception) {
        dfr.error(SimpleKotlinGraphQLError(e, listOf(mergedField.singleField.sourceLocation), path = executionStepInfo.path.toList()))
    }.build()
}
