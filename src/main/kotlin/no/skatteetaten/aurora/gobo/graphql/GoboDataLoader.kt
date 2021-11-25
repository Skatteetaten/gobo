package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.execution.KotlinDataLoader
import graphql.GraphQLContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderOptions

abstract class GoboDataLoader<K, V> : KotlinDataLoader<K, V> {

    abstract suspend fun getByKeys(keys: Set<K>, ctx: GraphQLContext): Map<K, V>

    override val dataLoaderName: String
        get() = this.javaClass.simpleName

    override fun getDataLoader(): DataLoader<K, V> =
        DataLoaderFactory.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.async(Dispatchers.IO) {
                    getByKeys(keys, (env.keyContexts.entries.first().value as GoboGraphQLContext).context)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}
