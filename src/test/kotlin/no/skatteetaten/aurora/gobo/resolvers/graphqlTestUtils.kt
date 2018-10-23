package no.skatteetaten.aurora.gobo.resolvers

import com.fasterxml.jackson.core.util.BufferRecyclers
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
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
