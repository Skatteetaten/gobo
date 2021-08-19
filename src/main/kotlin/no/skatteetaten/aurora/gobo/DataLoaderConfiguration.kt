package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.server.execution.KotlinDataLoader
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderOptions
import java.util.concurrent.Executors

private val coroutineDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

abstract class GoboDataLoader<K, V> : KotlinDataLoader<K, V> {

    abstract suspend fun getByKeys(keys: Set<K>, ctx: GoboGraphQLContext): Map<K, V>

    override val dataLoaderName: String
        get() = this.javaClass.simpleName

    override fun getDataLoader(): DataLoader<K, V> =
        DataLoader.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.async(coroutineDispatcher) {
                    getByKeys(keys, env.keyContexts.entries.first().value as GoboGraphQLContext)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}
