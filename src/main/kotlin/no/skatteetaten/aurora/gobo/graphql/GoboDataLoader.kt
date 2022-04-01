package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.execution.KotlinDataLoader
import graphql.GraphQLContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.slf4j.MDCContext
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.context.ApplicationContext

abstract class GoboDataLoader<K, V> : KotlinDataLoader<K, V> {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    abstract suspend fun getByKeys(keys: Set<K>, ctx: GraphQLContext): Map<K, V>

    override val dataLoaderName: String
        get() = this.javaClass.simpleName

    override fun getDataLoader(): DataLoader<K, V> {
        val tracer = applicationContext.getBean<Tracer>()
        println(tracer)
        return DataLoaderFactory.newMappedDataLoader(
            { keys: Set<K>, env: BatchLoaderEnvironment ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.async(
                    Dispatchers.IO + MDCContext() + applicationContext.getBean<Tracer>().asContextElement()
                ) {
                    getByKeys(keys, env.graphqlContext)
                }.asCompletableFuture()
            },
            DataLoaderOptions.newOptions().setCachingEnabled(false)
        )
    }
}

private val BatchLoaderEnvironment.graphqlContext
    get() = (keyContexts.entries.first().value as GoboGraphQLContext).context
