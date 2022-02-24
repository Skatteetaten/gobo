package no.skatteetaten.aurora.gobo

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.server.spring.GraphQLConfigurationProperties
import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboInstrumentation
import no.skatteetaten.aurora.gobo.graphql.QueryReporter
import no.skatteetaten.aurora.gobo.graphql.scalars.InstantScalar
import no.skatteetaten.aurora.gobo.graphql.scalars.JsonNodeScalar
import no.skatteetaten.aurora.gobo.graphql.scalars.KotlinLongScalar
import no.skatteetaten.aurora.gobo.graphql.scalars.UrlScalar
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.Resource
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.html
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import kotlin.reflect.KType

private val logger = KotlinLogging.logger { }

@Configuration
@EnableScheduling
class GraphQLConfig(
    private val config: GraphQLConfigurationProperties,
    @Value("classpath:/playground/gobo-graphql-playground.html") private val playgroundHtml: Resource,
    private val goboInstrumentation: GoboInstrumentation,
    private val queryReporter: QueryReporter,
    private val goboLiveness: GoboLiveness
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

        GET("/auroraapi") {
            temporaryRedirect(URI.create("/docs/index.html")).buildAndAwait()
        }

        GET("/liveness") {
            goboLiveness.isHealthy()
            ok().buildAndAwait()
        }
    }

    @Bean
    fun hooks() = GoboSchemaGeneratorHooks()

    /**
     * Wait 5 minutes for initial update, then wait 60 minutes between updates (can be configured)
     */
    @Scheduled(initialDelay = 300000, fixedDelayString = "\${gobo.graphqlUsage.fixedDelay:3600000}")
    fun updateGraphqlUsage() {
        logger.info { "Running scheduled job to update usage data at ${LocalDateTime.now()}" }
        goboInstrumentation.update()
        queryReporter.reportUnfinished()
    }
}

class GoboSchemaGeneratorHooks : SchemaGeneratorHooks {

    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier) {
        Long::class -> longType
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

    private val longType = GraphQLScalarType.newScalar()
        .name("Long")
        .description("A type representing kotlin.Long")
        .coercing(KotlinLongScalar)
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
