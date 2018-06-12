package no.skatteetaten.aurora.gobo.resolvers

import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import java.util.concurrent.CompletableFuture

interface KeysDataLoader<K, V> {
    fun getByKeys(keys: List<K>): List<V>
}

class NoCacheBatchDataLoader<K, V>(keysDataLoader: KeysDataLoader<K, V>) :
    DataLoader<K, V>(BatchLoader { keys: List<K> ->
        CompletableFuture.supplyAsync({
            keysDataLoader.getByKeys(keys)
        })
    }, DataLoaderOptions.newOptions().setCachingEnabled(false))
