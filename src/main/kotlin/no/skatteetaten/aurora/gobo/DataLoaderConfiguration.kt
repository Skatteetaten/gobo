package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.server.execution.DataLoaderRegistryFactory
import com.expediagroup.graphql.server.execution.KotlinDataLoader
import graphql.execution.DataFetcherResult
import kotlinx.coroutines.DelicateCoroutinesApi
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

@Deprecated(message = "Old way of configuring dataloaders", replaceWith = ReplaceWith("GoboDataLoader"))
interface KeyDataLoader<K, V> {
    suspend fun getByKey(key: K, context: GoboGraphQLContext): V
}

@Deprecated(message = "Old way of configuring dataloaders", replaceWith = ReplaceWith("GoboDataLoader"))
interface MultipleKeysDataLoader<K, V> {
    suspend fun getByKeys(keys: Set<K>, ctx: GoboGraphQLContext): Map<K, DataFetcherResult<V>>
}

private val coroutineDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

abstract class GoboDataLoader<K, V> : KotlinDataLoader<K, V> {

    abstract suspend fun getByKeys(keys: Set<K>, ctx: GoboGraphQLContext): Map<K, V>

    override val dataLoaderName: String
        get() = this.javaClass.simpleName

    override fun getDataLoader(): DataLoader<K, V> =
        DataLoader.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.async(coroutineDispatcher) {
                    getByKeys(keys, env.keyContexts.entries.first().value as GoboGraphQLContext)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}

private val logger = KotlinLogging.logger { }

@Configuration
class DataLoaderConfiguration(
    @Value("\${gobo.dataloader.thread-pool-size:4}") val threadPoolSize: Int,
    val keyLoaders: List<KeyDataLoader<*, *>>,
    val kotlinLoaders: List<KotlinDataLoader<*, *>>,
    val multipleKeysDataLoaders: List<MultipleKeysDataLoader<*, *>>
) {
    @Bean
    fun dataLoaderRegistryFactory(): DataLoaderRegistryFactory {
        val coroutineDispatcher = Executors.newFixedThreadPool(threadPoolSize).asCoroutineDispatcher()

        val kl = keyLoaders.map {
            logger.debug("Registering KeyDataLoader: ${it::class.simpleName}")
            it::class.simpleName!! to batchDataLoaderMappedSingle(coroutineDispatcher, it)
        }.toMap()

        val mkl = multipleKeysDataLoaders.map {
            logger.debug("Registering MultipleKeysDataLoader: ${it::class.simpleName}")
            it::class.simpleName!! to batchDataLoaderMappedMultiple(coroutineDispatcher, it)
        }.toMap()

        val dataLoaders = kl + mkl

        return object : DataLoaderRegistryFactory {
            override fun generate() = DataLoaderRegistry().apply {
                dataLoaders.forEach { register(it.key, it.value) }
                // TODO remove when the custom dataloader setup above can be removed, when all loaders are KotlinDataLoaders
                kotlinLoaders.forEach { register(it.dataLoaderName, it.getDataLoader()) }
            }
        }
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
