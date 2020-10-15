package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.hooks.SchemaGeneratorHooks
import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import no.skatteetaten.aurora.gobo.graphql.scalars.InstantScalar
import no.skatteetaten.aurora.gobo.graphql.scalars.JsonNodeScalar
import no.skatteetaten.aurora.gobo.graphql.scalars.UrlScalar
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL
import java.time.Instant
import kotlin.reflect.KType

@Configuration
class GraphQLConfig {

    @Bean
    fun hooks() = GoboSchemaGeneratorHooks()
}

class GoboSchemaGeneratorHooks : SchemaGeneratorHooks {

    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier) {
        Instant::class -> instantType
        URL::class -> urlType
        JsonNode::class -> jsonNodeType
        else -> null
    }

    private val instantType = GraphQLScalarType.newScalar()
        .name("Instant")
        .description("A type representing java.time.Instant")
        .coercing(InstantScalar)
        .build()

    private val jsonNodeType = GraphQLScalarType.newScalar()
        .name("JsonNode")
        .description("A type representing com.fasterxml.jackson.databind.JsonNode")
        .coercing(JsonNodeScalar)
        .build()

    private val urlType = GraphQLScalarType.newScalar()
        .name("URL")
        .description("A type representing java.net.URL")
        .coercing(UrlScalar)
        .build()
}

/*
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
