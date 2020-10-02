package no.skatteetaten.aurora.gobo.resolvers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.future.await
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.time.debounce
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry

interface KtDataLoaderRegistryFactory {
    fun build(scope: CoroutineScope): DataLoaderRegistry
}

class KtDataLoaderRegistry(
    private val registry: DataLoaderRegistry,
    private val dispatchAfterLoad: suspend () -> Unit
) {
    fun <K, V> getDataLoader(key: String) = KtDataLoader(
        registry.getDataLoader<K, V>(key),
        dispatchAfterLoad
    )
}

class KtDataLoader<K, V>(
    private val dataLoader: DataLoader<K, V>,
    private val dispatchAfterLoad: suspend () -> Unit
) {
    suspend fun load(key: K): V = dataLoader.load(key).also {
        dispatchAfterLoad.invoke()
    }.await()

    suspend fun loadMany(keys: List<K>): List<V> = dataLoader.loadMany(keys).also {
        dispatchAfterLoad.invoke()
    }.await()

    fun clear(key: K) = dataLoader.clear(key)
    fun clearAll() = dataLoader.clearAll()
    fun prime(key: K, value: V) = dataLoader.prime(key, value)
    fun prime(key: K, exception: Exception) = dataLoader.prime(key, exception)
}

class KtDataLoaderDispatcher(
    private val dataLoaderRegistryFactory: KtDataLoaderRegistryFactory
) {
    suspend fun <R> run(block: suspend (registry: KtDataLoaderRegistry) -> R): R {
        var result: R? = null

        supervisorScope {
            val registry = dataLoaderRegistryFactory.build(this)

            channelFlow<Unit> {
                val wrappedRegistry = KtDataLoaderRegistry(registry) { channel.send(Unit) }
                result = block(wrappedRegistry)
            }
                .debounce(1)
                .collect { registry.dispatchAll() }
        }

        return result!!
    }
}