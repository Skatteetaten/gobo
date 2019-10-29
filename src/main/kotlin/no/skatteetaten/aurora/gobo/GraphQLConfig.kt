package no.skatteetaten.aurora.gobo

import com.coxautodev.graphql.tools.SchemaParserOptions
import com.oembedler.moon.graphql.boot.GraphQLWebAutoConfiguration.MUTATION_EXECUTION_STRATEGY
import com.oembedler.moon.graphql.boot.GraphQLWebAutoConfiguration.QUERY_EXECUTION_STRATEGY
import com.oembedler.moon.graphql.boot.GraphQLWebAutoConfiguration.SUBSCRIPTION_EXECUTION_STRATEGY
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.ExecutionStrategy
import graphql.execution.SubscriptionExecutionStrategy
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.tracing.TracingInstrumentation
import no.skatteetaten.aurora.gobo.resolvers.errorhandling.GoboDataFetcherExceptionHandler
import org.dataloader.Try
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GraphQLConfig {

    @Bean
    fun schemaParserOptions(): SchemaParserOptions =
        SchemaParserOptions
            .newOptions()
            .genericWrappers(listOf(SchemaParserOptions.GenericWrapper(Try::class.java, 0)))
            .build()

    @Bean
    fun executionStrategies(): Map<String, ExecutionStrategy> {
        val exceptionHandler = GoboDataFetcherExceptionHandler()
        return mapOf(
            QUERY_EXECUTION_STRATEGY to AsyncExecutionStrategy(exceptionHandler),
            MUTATION_EXECUTION_STRATEGY to AsyncExecutionStrategy(exceptionHandler),
            SUBSCRIPTION_EXECUTION_STRATEGY to SubscriptionExecutionStrategy(exceptionHandler)
        )
    }

    @Bean
    @ConditionalOnProperty(name = ["gobo.graphql.tracing-enabled"], havingValue = "true", matchIfMissing = false)
    fun tracingInstrumentation(): Instrumentation = TracingInstrumentation()
}
