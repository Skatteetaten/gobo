package no.skatteetaten.aurora.gobo.resolvers

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.DataLoader
import org.dataloader.Try
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

interface KeyDataLoader<K, V> {
    fun getByKey(user: User, key: K): Try<V>
}

val context = Executors.newFixedThreadPool(6).asCoroutineDispatcher()

/*
  Use this if you have a service that loads a single id. Will make requests in parallel
 */
fun <K, V> batchDataLoaderMappedSingle(user: User, keyDataLoader: KeyDataLoader<K, V>): DataLoader<K, V> =
    DataLoader.newMappedDataLoaderWithTry { keys: Set<K> ->
        CompletableFuture.supplyAsync {
            runBlocking(context) {
                val deferred: List<Deferred<Pair<K, Try<V>>>> = keys.map { key ->
                    async(context) {
                        key to keyDataLoader.getByKey(user, key)
                    }
                }
                deferred.map { it.await() }.toMap()
            }
        }
    }

interface MultipleKeysDataLoader<K, V> {
    fun getByKeys(user: User, keys: MutableSet<K>): Map<K, Try<V>>
}

/*
  Use this if you have a service that loads multiple ids.
 */
fun <K, V> batchDataLoaderMappedMultiple(user: User, keysDataLoader: MultipleKeysDataLoader<K, V>): DataLoader<K, V> =
    DataLoader.newMappedDataLoaderWithTry { keys: MutableSet<K> ->
        CompletableFuture.supplyAsync {
            keysDataLoader.getByKeys(user, keys)
        }
    }
