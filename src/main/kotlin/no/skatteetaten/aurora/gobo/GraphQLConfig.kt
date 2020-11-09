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
