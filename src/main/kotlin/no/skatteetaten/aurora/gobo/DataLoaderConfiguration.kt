package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.spring.execution.DataLoaderRegistryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asCompletableFuture
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.resolvers.KtDataLoaderRegistryFactory
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoader.newDataLoader
import org.dataloader.DataLoaderOptions
import org.dataloader.DataLoaderRegistry
import org.dataloader.Try
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

interface KeyDataLoader<K, V> {
    suspend fun getByKey(key: K, context: GoboGraphQLContext): V
}

interface MultipleKeysDataLoader<K, V> {
    suspend fun getByKeys(keys: Set<K>, ctx: GoboGraphQLContext): Map<K, Try<V>>
}

private val logger = KotlinLogging.logger { }

@Configuration
class DataLoaderConfiguration(
    val keyLoaders: List<KeyDataLoader<*, *>>,
    val multipleKeysDataLoaders: List<MultipleKeysDataLoader<*, *>>
) {
    @Bean
    fun dataLoaderRegistryFactory(): DataLoaderRegistryFactory {
        val registry = DataLoaderRegistry().apply {
            keyLoaders.forEach {
                logger.debug("Registering KeyDataLoader: ${it::class.simpleName}")
                register(it::class.simpleName, batchDataLoaderMappedSingle(it))
            }
            multipleKeysDataLoaders.forEach {
                logger.debug("Registering MultipleKeysDataLoader: ${it::class.simpleName}")
                register(it::class.simpleName, batchDataLoaderMappedMultiple(it))
            }
        }



        return object : DataLoaderRegistryFactory {
            override fun generate() = registry
        }
    }

    /**
     * Use this if you have a service that loads multiple ids.
     */
    private fun <K, V> batchDataLoaderMappedMultiple(keysDataLoader: MultipleKeysDataLoader<K, V>): DataLoader<K, V> =
        DataLoader.newMappedDataLoaderWithTry(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                GlobalScope.async {
                    keysDataLoader.getByKeys(keys, env.keyContexts.entries.first().value as GoboGraphQLContext)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )

    /**
     * Use this if you have a service that loads a single id. Will make requests in parallel
     */
    private fun <K, V> batchDataLoaderMappedSingle(keyDataLoader: KeyDataLoader<K, V>): DataLoader<K, V> =
        DataLoader.newMappedDataLoaderWithTry(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                GlobalScope.async {
                    val deferred: List<Deferred<Pair<K, Try<V>>>> = keys.map { key ->
                        async {
                            key to try {
                                val ctx = env.keyContexts[key] as GoboGraphQLContext
                                Try.succeeded(keyDataLoader.getByKey(key, ctx))
                            } catch (e: Exception) {
                                Try.failed<V>(e)
                            }
                        }
                    }
                    deferred.awaitAll().toMap()
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}
