package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.GraphQLContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderOptions
import java.util.concurrent.TimeUnit

abstract class GoboDataLoader<K, V> : KotlinDataLoader<K, V> {

    abstract suspend fun getByKeys(keys: Set<K>, ctx: GraphQLContext): Map<K, V>

    override val dataLoaderName: String
        get() = this.javaClass.simpleName

    override fun getDataLoader(): DataLoader<K, V> =
        DataLoaderFactory.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.future(Dispatchers.Unconfined + MDCContext() + TracingContextElement()) {
                    getByKeys(keys, env.graphqlContext)
                }.orTimeout(3, TimeUnit.MINUTES)
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}

private val BatchLoaderEnvironment.graphqlContext
    get() = (keyContexts.entries.first().value as GoboGraphQLContext).context
