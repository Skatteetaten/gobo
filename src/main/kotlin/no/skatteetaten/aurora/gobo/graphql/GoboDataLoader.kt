package no.skatteetaten.aurora.gobo.graphql

import brave.Tracing
import brave.propagation.CurrentTraceContext
import com.expediagroup.graphql.server.execution.KotlinDataLoader
import graphql.GraphQLContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderOptions
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// Implemented based on https://github.com/openzipkin/brave/issues/820#issuecomment-447614394
private class TracingContextElement : ThreadContextElement<CurrentTraceContext.Scope?>, AbstractCoroutineContextElement(Key) {
    private val currentTraceContext: CurrentTraceContext? = Tracing.current()?.currentTraceContext()
    private val initial = currentTraceContext?.get()
    companion object Key : CoroutineContext.Key<TracingContextElement>

    override fun updateThreadContext(context: CoroutineContext): CurrentTraceContext.Scope? {
        return currentTraceContext?.maybeScope(initial)
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: CurrentTraceContext.Scope?) {
        oldState?.close()
    }
}

abstract class GoboDataLoader<K, V> : KotlinDataLoader<K, V> {

    abstract suspend fun getByKeys(keys: Set<K>, ctx: GraphQLContext): Map<K, V>

    override val dataLoaderName: String
        get() = this.javaClass.simpleName

    override fun getDataLoader(): DataLoader<K, V> =
        DataLoaderFactory.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.async(Dispatchers.IO + MDCContext() + TracingContextElement()) {
                    getByKeys(keys, env.graphqlContext)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
}

private val BatchLoaderEnvironment.graphqlContext
    get() = (keyContexts.entries.first().value as GoboGraphQLContext).context
