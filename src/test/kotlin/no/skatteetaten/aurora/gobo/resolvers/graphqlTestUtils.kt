package no.skatteetaten.aurora.gobo.resolvers

import com.fasterxml.jackson.core.util.BufferRecyclers
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.JsonPathAssertions
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.StreamUtils
import org.springframework.web.reactive.function.BodyInserters
import java.nio.charset.StandardCharsets

private fun query(payload: String, variables: String) = """
{
  "query": "$payload",
  "variables": $variables
}
"""

fun createQuery(queryResource: Resource, variables: Map<String, *> = emptyMap<String, String>()): String {
    val query = StreamUtils.copyToString(queryResource.inputStream, StandardCharsets.UTF_8)
    return createQuery(query, variables)
}

fun createQuery(query: String, variables: Map<String, *> = emptyMap<String, String>()): String {
    val json = BufferRecyclers
        .getJsonStringEncoder()
        .quoteAsString(query)
        .joinToString("")

    val variablesJson = jacksonObjectMapper().writeValueAsString(variables)
    return query(json, variablesJson)
}

fun WebTestClient.queryGraphQL(
    queryResource: Resource,
    variables: Map<String, *> = emptyMap<String, String>(),
    token: String? = null
): WebTestClient.ResponseSpec {
    val query = createQuery(queryResource, variables)
    val requestSpec = this.post().uri("/graphql")
    token?.let {
        requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    return requestSpec
        .body(BodyInserters.fromObject(query))
        .exchange()
}

private val logger = KotlinLogging.logger { }
fun WebTestClient.BodyContentSpec.logResult() {
    this.returnResult().responseBody?.let {
        logger.info { "Response body: $it" }
    }
}

fun WebTestClient.BodyContentSpec.graphqlDataWithPrefix(
    prefix: String,
    fn: GraphqlDataWithPrefix.() -> Unit
): WebTestClient.BodyContentSpec {
    fn(GraphqlDataWithPrefix(prefix, this))
    return this
}

fun WebTestClient.BodyContentSpec.graphqlDataWithPrefixAndIndex(
    prefix: String,
    startIndex: Int = 0,
    endIndex: Int = 0,
    fn: GraphqlDataWithPrefixAndIndex.() -> Unit
): WebTestClient.BodyContentSpec {
    for (i in startIndex..endIndex) {
        fn(GraphqlDataWithPrefixAndIndex(prefix, i, this))
    }
    return this
}

class GraphqlDataWithPrefix(private val prefix: String, private val bodyContentSpec: WebTestClient.BodyContentSpec) {
    fun graphqlData(jsonPath: String) = bodyContentSpec.graphqlJsonPath(jsonPath, "data.$prefix")
    fun graphqlDataFirst(jsonPath: String) = bodyContentSpec.graphqlJsonPath(jsonPath, "data.$prefix[0]")
}

class GraphqlDataWithPrefixAndIndex(
    private val prefix: String,
    val index: Int,
    private val bodyContentSpec: WebTestClient.BodyContentSpec
) {
    fun graphqlData(jsonPath: String) = bodyContentSpec.graphqlJsonPath(jsonPath, "data.$prefix[$index]")
}

fun WebTestClient.BodyContentSpec.graphqlData(jsonPath: String) =
    graphqlJsonPath(jsonPath, "data")

fun WebTestClient.BodyContentSpec.graphqlErrors(jsonPath: String) =
    graphqlJsonPath(jsonPath, "errors")

fun WebTestClient.BodyContentSpec.graphqlErrorsFirst(jsonPath: String) =
    graphqlJsonPath(jsonPath, "errors[0]")

private fun WebTestClient.BodyContentSpec.graphqlJsonPath(jsonPath: String, type: String): JsonPathAssertions {
    val expression = if (jsonPath.startsWith("[")) {
        "$.$type$jsonPath"
    } else {
        "$.$type.$jsonPath"
    }
    return this.jsonPath(expression)
}

fun JsonPathAssertions.isTrue() = this.isEqualTo(true)
fun JsonPathAssertions.isFalse() = this.isEqualTo(false)
