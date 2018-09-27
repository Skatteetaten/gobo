package no.skatteetaten.aurora.gobo.resolvers

import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import reactor.core.publisher.Flux
import java.util.concurrent.CompletableFuture

interface KeysDataLoader<K, V> {
    fun getByKeys(keys: List<K>): List<V>
}

interface KeysDataLoaderFlux<K, V> {
    fun getByKeys(keys: List<K>): Flux<V>
}


class NoCacheBatchDataLoader<K, V>(keysDataLoader: KeysDataLoader<K, V>) :
    DataLoader<K, V>(BatchLoader { keys: List<K> ->
        CompletableFuture.supplyAsync {
            keysDataLoader.getByKeys(keys)
        }
    }, DataLoaderOptions.newOptions().setCachingEnabled(false))

class NoCacheBatchDataLoaderFlux<K, V>(keysDataLoader: KeysDataLoaderFlux<K, V>) :
    DataLoader<K, V>(BatchLoader { keys: List<K> ->
        keysDataLoader.getByKeys(keys).collectList().toFuture()
    }, DataLoaderOptions.newOptions().setCachingEnabled(false))
