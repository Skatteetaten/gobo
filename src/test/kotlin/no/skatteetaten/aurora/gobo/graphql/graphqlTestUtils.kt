package no.skatteetaten.aurora.gobo.graphql

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.webflux.AuroraRequestParser.KLIENTID_FIELD
import no.skatteetaten.aurora.webflux.AuroraRequestParser.KORRELASJONSID_FIELD
import org.apache.commons.text.StringEscapeUtils
import org.hamcrest.Matchers
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.JsonPathAssertions
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.StreamUtils
import org.springframework.web.reactive.function.BodyInserters
import java.nio.charset.StandardCharsets

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
    input: Any,
    token: String? = null
) = queryGraphQL(
    queryResource,
    mapOf("input" to jacksonObjectMapper().convertValue<Map<String, Any>>(input)),
    token
)

fun WebTestClient.queryGraphQL(
    queryResource: Resource,
    variables: Map<String, *> = emptyMap<String, String>(),
    token: String? = null
): WebTestClient.ResponseSpec {
    val query = createQuery(queryResource, variables)
    val requestSpec = this.post().uri("/graphql")
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .header(KORRELASJONSID_FIELD, "unit-test")
        .header(KLIENTID_FIELD, "gobo")
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

fun WebTestClient.BodyContentSpec.graphqlErrorsFirstContainsMessage(message: String): WebTestClient.BodyContentSpec =
    this.graphqlErrorsFirst("message").contains(message)

fun JsonPathAssertions.contains(value: String) =
    this.value(Matchers.containsString(value))

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
