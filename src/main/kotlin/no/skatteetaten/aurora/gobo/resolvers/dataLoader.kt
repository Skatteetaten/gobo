package no.skatteetaten.aurora.gobo.resolvers

import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.future.await
import org.dataloader.Try

interface KeyDataLoader<K, V> {
    fun getByKey(key: K, context: GoboGraphQLContext): Try<V>
}

/*
  Load a single key of type Key into a value of type Value using a dataloader named <Value>DataLoader

  If the loading throws and error the entire query will fail
 */
suspend inline fun <Key, reified Value> DataFetchingEnvironment.loadMany(key: Key): List<Value> {
    val loaderName = "${Value::class.java.simpleName}ListDataLoader"
    return this.getDataLoader<Key, List<Value>>(loaderName).load(key, this.getContext()).await()
}

/*
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
*/
