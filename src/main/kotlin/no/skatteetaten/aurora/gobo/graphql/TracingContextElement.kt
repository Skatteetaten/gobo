package no.skatteetaten.aurora.gobo.graphql

import brave.Tracing
import brave.propagation.CurrentTraceContext
import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// Implemented based on https://github.com/openzipkin/brave/issues/820#issuecomment-447614394
class TracingContextElement : ThreadContextElement<CurrentTraceContext.Scope?>,
    AbstractCoroutineContextElement(Key) {
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
