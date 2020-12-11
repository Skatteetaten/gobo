package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.spring.GraphQLConfigurationProperties
import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import no.skatteetaten.aurora.gobo.graphql.GoboInstrumentation
import no.skatteetaten.aurora.gobo.graphql.scalars.InstantScalar
import no.skatteetaten.aurora.gobo.graphql.scalars.JsonNodeScalar
import no.skatteetaten.aurora.gobo.graphql.scalars.UrlScalar
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.Resource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.html
import java.net.URL
import java.time.Instant
import kotlin.reflect.KType

@Configuration
class GraphQLConfig(
    private val config: GraphQLConfigurationProperties,
    @Value("classpath:/playground/gobo-graphql-playground.html") private val playgroundHtml: Resource,
    private val goboInstrumentation: GoboInstrumentation
) {
    private val body = playgroundHtml.inputStream.bufferedReader().use { reader ->
        reader.readText()
            .replace("\${graphQLEndpoint}", config.endpoint)
            .replace("\${subscriptionsEndpoint}", config.subscriptions.endpoint)
    }

    @Bean
    @Primary
    fun playgroundRouteGobo() = coRouter {
        GET(config.playground.endpoint) {
            ok().html().bodyValueAndAwait(body)
        }
    }

    @Bean
    fun hooks() = GoboSchemaGeneratorHooks()

    /**
     * Updates the usage data every 10 minutes by default
     */
    @Scheduled(fixedRateString = "\${gobo.graphqlUsage.fixedRate:600000}")
    fun updateGraphqlUsage() {
        goboInstrumentation.update()
    }
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
