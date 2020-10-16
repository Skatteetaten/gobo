package no.skatteetaten.aurora.gobo.graphql.scalars

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingSerializeException

object JsonNodeScalar : Coercing<JsonNode, String> {

    override fun serialize(input: Any?): String = input.toString()

    override fun parseValue(input: Any?): JsonNode = parseLiteral(input)

    override fun parseLiteral(input: Any?): JsonNode =
        when (input) {
            is StringValue -> jacksonObjectMapper().readTree(input.value)
            else -> throw CoercingSerializeException("Invalid value '$input' for JsonNode")
        }
}
