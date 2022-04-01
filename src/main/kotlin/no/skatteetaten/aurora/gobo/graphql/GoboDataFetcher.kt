package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.generator.execution.FunctionDataFetcher
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetcherFactory
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.getBean
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.context.ApplicationContext
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaType

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
) : FunctionDataFetcher(target, fn, objectMapper, MDCContext() + applicationContext.getBean<Tracer>().asContextElement()) {

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
