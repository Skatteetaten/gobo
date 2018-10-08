package no.skatteetaten.aurora.gobo

import com.coxautodev.graphql.tools.SchemaParserOptions
import com.oembedler.moon.graphql.boot.GraphQLWebAutoConfiguration
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.ExecutionStrategy
import graphql.execution.SubscriptionExecutionStrategy
import no.skatteetaten.aurora.gobo.resolvers.errorhandling.GoboDataFetcherExceptionHandler
import org.dataloader.Try
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
    fun executionStrategies(): Map<String, ExecutionStrategy> =
            mapOf(
                    GraphQLWebAutoConfiguration.QUERY_EXECUTION_STRATEGY to AsyncExecutionStrategy(GoboDataFetcherExceptionHandler()),
                    GraphQLWebAutoConfiguration.MUTATION_EXECUTION_STRATEGY to AsyncExecutionStrategy(),
                    GraphQLWebAutoConfiguration.SUBSCRIPTION_EXECUTION_STRATEGY to SubscriptionExecutionStrategy()
            )
}