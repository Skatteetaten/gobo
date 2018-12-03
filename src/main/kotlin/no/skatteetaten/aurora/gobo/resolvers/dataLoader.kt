package no.skatteetaten.aurora.gobo.resolvers

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import org.dataloader.Try
import java.util.concurrent.CompletableFuture

interface KeyDataLoader<K, V> {
    fun getByKey(user: User, key: K): Try<V>
}

val context = newFixedThreadPoolContext(6, "dataloader")

/*
  You this if you have a service that loads a single id. Will make requests in parallel
 */
fun <K, V> noCacheBatchDataLoaderMappedSingle(user: User, keyDataLoader: KeyDataLoader<K, V>): DataLoader<K, V> =
    DataLoader.newMappedDataLoaderWithTry({ keys: Set<K> ->

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
    }, DataLoaderOptions.newOptions().setCachingEnabled(false))
