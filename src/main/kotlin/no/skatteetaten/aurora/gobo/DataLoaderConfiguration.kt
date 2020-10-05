package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.spring.execution.DataLoaderRegistryFactory
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import org.dataloader.DataLoaderRegistry
import org.dataloader.Try
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

interface KeyDataLoader<K, V> {
    suspend fun getByKey(key: K, context: GoboGraphQLContext): V
}

interface MultipleKeysDataLoader<K, V> {
    suspend fun getByKeys(keys: Set<K>, ctx: GoboGraphQLContext): Map<K, Try<V>>
}

private val logger = KotlinLogging.logger { }

@Configuration
class DataLoaderConfiguration(
    @Value("\${gobo.dataloader.thread-pool-size:4}") val threadPoolSize: Int,
    val keyLoaders: List<KeyDataLoader<*, *>>,
    val multipleKeysDataLoaders: List<MultipleKeysDataLoader<*, *>>
) {
    @Bean
    fun dataLoaderRegistryFactory(): DataLoaderRegistryFactory {
        val coroutineDispatcher = Executors.newFixedThreadPool(threadPoolSize).asCoroutineDispatcher()
        val registry = DataLoaderRegistry().apply {
            keyLoaders.forEach {
                logger.debug("Registering KeyDataLoader: ${it::class.simpleName}")
                register(it::class.simpleName, batchDataLoaderMappedSingle(coroutineDispatcher, it))
            }
            multipleKeysDataLoaders.forEach {
                logger.debug("Registering MultipleKeysDataLoader: ${it::class.simpleName}")
                register(it::class.simpleName, batchDataLoaderMappedMultiple(coroutineDispatcher, it))
            }
        }



        return object : DataLoaderRegistryFactory {
            override fun generate() = registry
        }
    }

    /**
     * Use this if you have a service that loads multiple ids.
     */
    private fun <K, V> batchDataLoaderMappedMultiple(
        coroutineDispatcher: ExecutorCoroutineDispatcher,
        keysDataLoader: MultipleKeysDataLoader<K, V>
    ): DataLoader<K, V> =
        DataLoader.newMappedDataLoaderWithTry(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                GlobalScope.async(coroutineDispatcher) {
                    keysDataLoader.getByKeys(keys, env.keyContexts.entries.first().value as GoboGraphQLContext)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )

    /**
     * Use this if you have a service that loads a single id. Will make requests in parallel
     */
    private fun <K, V> batchDataLoaderMappedSingle(
        coroutineDispatcher: ExecutorCoroutineDispatcher,
        keyDataLoader: KeyDataLoader<K, V>
    ): DataLoader<K, V> =
        DataLoader.newMappedDataLoaderWithTry(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                GlobalScope.async(coroutineDispatcher) {
                    keys.map { key ->
                        key to try {
                            val ctx = env.keyContexts[key] as GoboGraphQLContext
                            Try.succeeded(keyDataLoader.getByKey(key, ctx))
                        } catch (e: Exception) {
                            Try.failed<V>(e)
                        }
                    }.toMap()
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}
