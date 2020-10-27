package no.skatteetaten.aurora.gobo.graphql

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import org.apache.commons.text.StringEscapeUtils
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.JsonPathAssertions
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.StreamUtils
import org.springframework.web.reactive.function.BodyInserters

private fun query(payload: String, variables: String) =
    """
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
    val json = StringEscapeUtils.escapeJson(query)
    val variablesJson = jacksonObjectMapper().writeValueAsString(variables)
    return query(json, variablesJson)
}

fun WebTestClient.queryGraphQL(
    queryResource: Resource,
    variables: Map<String, *> = emptyMap<String, String>(),
    token: String? = null
): WebTestClient.ResponseSpec {
    val query = createQuery(queryResource, variables)
    val requestSpec = this.post().uri("/graphql").header("Content-Type", "application/json")
    token?.let {
        requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    return requestSpec
        .body(BodyInserters.fromValue(query))
        .exchange()
}

fun WebTestClient.BodyContentSpec.printResult() {
    this.returnResult().responseBody?.let {
        println("Response body: ${String(it)}")
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

fun WebTestClient.BodyContentSpec.graphqlDoesNotContainErrors() =
    this.jsonPath("$.errors").doesNotExist()

fun WebTestClient.BodyContentSpec.graphqlData(jsonPath: String) =
    graphqlJsonPath(jsonPath, "data")

fun WebTestClient.BodyContentSpec.graphqlErrors(jsonPath: String) =
    graphqlJsonPath(jsonPath, "errors")

fun WebTestClient.BodyContentSpec.graphqlErrorsFirst(jsonPath: String) =
    graphqlJsonPath(jsonPath, "errors[0]")

fun WebTestClient.BodyContentSpec.graphqlErrorsMissingToken() =
    graphqlErrorsFirst("message").isEqualTo("Token is not set")

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
