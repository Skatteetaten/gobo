package no.skatteetaten.aurora.gobo.graphql

import brave.Tracing
import brave.propagation.CurrentTraceContext
import com.expediagroup.graphql.generator.execution.FunctionDataFetcher
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetcherFactory
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaType

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

class GoboSpringKotlinDataFetcherFactoryProvider(
    private val objectMapper: ObjectMapper,
    private val applicationContext: ApplicationContext,
) : SimpleKotlinDataFetcherFactoryProvider(objectMapper = objectMapper) {
    override fun functionDataFetcherFactory(target: Any?, kFunction: KFunction<*>): DataFetcherFactory<Any?> =
        DataFetcherFactory { GoboDataFetcher(target, kFunction, objectMapper, applicationContext) }
}


// Code taken from SpringDataFetcher in graphql-kotlin
class GoboDataFetcher(
    target: Any?,
    fn: KFunction<*>,
    objectMapper: ObjectMapper,
    private val applicationContext: ApplicationContext
) : FunctionDataFetcher(target, fn, objectMapper, MDCContext() + TracingContextElement()) {

    override fun mapParameterToValue(param: KParameter, environment: DataFetchingEnvironment): Pair<KParameter, Any?>? =
        if (param.hasAnnotation<Autowired>()) {
            val qualifier = param.findAnnotation<Qualifier>()?.value
            if (qualifier != null) {
                param to applicationContext.getBean(qualifier)
            } else {
                param to applicationContext.getBean(param.type.javaType as Class<*>)
            }
        } else {
            super.mapParameterToValue(param, environment)
        }
}
