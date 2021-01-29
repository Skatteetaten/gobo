package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.spring.execution.DataLoaderRegistryFactory
import graphql.execution.DataFetcherResult
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GraphQLExceptionWrapper
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import org.dataloader.DataLoaderRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

interface KeysBatchDataLoader<K, V> {
    suspend fun getByKeys(keys: Set<K>, context: GoboGraphQLContext): Map<K, V>
}

interface KeyDataLoader<K, V> {
    suspend fun getByKey(key: K, context: GoboGraphQLContext): V
}

interface MultipleKeysDataLoader<K, V> {
    suspend fun getByKeys(keys: Set<K>, ctx: GoboGraphQLContext): Map<K, DataFetcherResult<V>>
}

private val logger = KotlinLogging.logger { }

@Configuration
class DataLoaderConfiguration(
    @Value("\${gobo.dataloader.thread-pool-size:4}") val threadPoolSize: Int,
    val keyLoaders: List<KeyDataLoader<*, *>>,
    val multipleKeysDataLoaders: List<MultipleKeysDataLoader<*, *>>,
    val keysBatchLoaders: List<KeysBatchDataLoader<*, *>>
) {
    @Bean
    fun dataLoaderRegistryFactory(): DataLoaderRegistryFactory {
        val coroutineDispatcher = Executors.newFixedThreadPool(threadPoolSize).asCoroutineDispatcher()

        val kbl = keysBatchLoaders.map {
            logger.debug("Registering KeysBatchDataLoader: ${it::class.simpleName}")
            it::class.simpleName!! to batchDataLoader(coroutineDispatcher, it)
        }.toMap()

        val kl = keyLoaders.map {
            logger.debug("Registering KeyDataLoader: ${it::class.simpleName}")
            it::class.simpleName!! to batchDataLoaderMappedSingle(coroutineDispatcher, it)
        }.toMap()

        val mkl = multipleKeysDataLoaders.map {
            logger.debug("Registering MultipleKeysDataLoader: ${it::class.simpleName}")
            it::class.simpleName!! to batchDataLoaderMappedMultiple(coroutineDispatcher, it)
        }.toMap()

        val dataLoaders = kbl + kl + mkl
        return object : DataLoaderRegistryFactory {
            override fun generate() = DataLoaderRegistry().apply {
                dataLoaders.forEach { register(it.key, it.value) }
            }
        }
    }

    /**
     * Use this if you have a service that can load multiple ids in one request.
     */
    private fun <K, V> batchDataLoader(
        coroutineDispatcher: ExecutorCoroutineDispatcher,
        dataLoader: KeysBatchDataLoader<K, V>
    ): DataLoader<K, V> {
        return DataLoader.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                GlobalScope.async(coroutineDispatcher) {
                    dataLoader.getByKeys(keys, env.keyContexts.entries.first().value as GoboGraphQLContext)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
    }

    /**
     * Use this if you have a service that loads multiple ids.
     */
    private fun <K, V> batchDataLoaderMappedMultiple(
        coroutineDispatcher: ExecutorCoroutineDispatcher,
        keysDataLoader: MultipleKeysDataLoader<K, V>
    ): DataLoader<K, DataFetcherResult<V>> =
        DataLoader.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                GlobalScope.async(coroutineDispatcher) {
                    keysDataLoader.getByKeys(
                        keys,
                        env.keyContexts.entries.first().value as GoboGraphQLContext
                    )
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false).setBatchingEnabled(false)
        )

    /**
     * Use this if you have a service that loads a single id. Will make requests in parallel
     */
    private fun <K, V> batchDataLoaderMappedSingle(
        coroutineDispatcher: ExecutorCoroutineDispatcher,
        keyDataLoader: KeyDataLoader<K, V>
    ): DataLoader<K, DataFetcherResult<V>> =
        DataLoader.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                GlobalScope.async(coroutineDispatcher) {
                    keys.map { key ->
                        key to DataFetcherResult.newResult<V>().apply {
                            try {
                                val ctx = env.keyContexts[key] as GoboGraphQLContext
                                data(keyDataLoader.getByKey(key, ctx))
                            } catch (e: Exception) {
                                error(GraphQLExceptionWrapper(e))
                            }
                        }.build()
                    }.toMap()
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}
