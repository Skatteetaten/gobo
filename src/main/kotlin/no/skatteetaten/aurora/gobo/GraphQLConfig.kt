package no.skatteetaten.aurora.gobo

/*
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
*/
