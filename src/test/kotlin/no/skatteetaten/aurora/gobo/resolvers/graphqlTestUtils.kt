package no.skatteetaten.aurora.gobo.resolvers

import com.fasterxml.jackson.core.util.BufferRecyclers
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.io.Resource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

private fun query(payload: String, variables: String) = """
{
  "query": "$payload",
  "variables": $variables
}
"""

fun createQuery(queryResource: Resource, variables: Map<String, *> = emptyMap<String, String>()): String {
    val query = StreamUtils.copyToString(queryResource.inputStream, StandardCharsets.UTF_8)
    val json = BufferRecyclers
        .getJsonStringEncoder()
        .quoteAsString(query)
        .joinToString("")

    val variablesJson = jacksonObjectMapper().writeValueAsString(variables)
    return query(json, variablesJson)
}
