package no.skatteetaten.aurora.gobo.graphql.scalars

import graphql.language.IntValue
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingSerializeException
import java.time.Instant

object InstantScalar : Coercing<Instant, String> {

    override fun serialize(input: Any?): String = when (input) {
        is Long -> Instant.ofEpochMilli(input).toString()
        else -> (input as Instant).toString()
    }

    override fun parseValue(input: Any?): Instant = parseLiteral(input)

    override fun parseLiteral(input: Any?): Instant =
        when (input) {
            is StringValue -> Instant.parse(input.value)
            is IntValue -> Instant.ofEpochMilli(input.value.longValueExact())
            else -> throw CoercingSerializeException("Invalid value '$input' for LocalTime")
        }
}
