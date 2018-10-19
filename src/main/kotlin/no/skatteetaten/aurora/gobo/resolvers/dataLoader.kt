package no.skatteetaten.aurora.gobo.resolvers

import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import reactor.core.publisher.Flux
import java.util.concurrent.CompletableFuture

interface KeysDataLoader<K, V> {
    fun getByKeys(user: User, keys: List<K>): List<V>
}

interface KeysDataLoaderFlux<K, V> {
    fun getByKeys(user: User, keys: List<K>): Flux<V>
}

fun <K, V> createNoCacheBatchDataLoader(user: User, keysDataLoader: KeysDataLoader<K, V>): DataLoader<K, V> =
    DataLoader.newDataLoader({ keys: List<K> ->
        CompletableFuture.supplyAsync {
            keysDataLoader.getByKeys(user, keys)
        }
    }, DataLoaderOptions.newOptions().setCachingEnabled(false))

fun <K, V> createNoCacheBatchDataLoaderFlux(user: User, keysDataLoader: KeysDataLoaderFlux<K, V>): DataLoader<K, V> =
    DataLoader.newDataLoader({ keys: List<K> ->
        keysDataLoader.getByKeys(user, keys).collectList().toFuture()
    }, DataLoaderOptions.newOptions().setCachingEnabled(false))
