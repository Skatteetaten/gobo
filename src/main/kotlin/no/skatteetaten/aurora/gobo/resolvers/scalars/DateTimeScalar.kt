package no.skatteetaten.aurora.gobo.resolvers.scalars

import graphql.language.IntValue
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DateTimeScalar : GraphQLScalarType(
    "DateTime", "DateTime scalar", (object : Coercing<Instant, String> {

        override fun serialize(input: Any?) = (input as Instant).toString()

        override fun parseValue(input: Any?): Instant = parseLiteral(input)

        override fun parseLiteral(input: Any?) =
            when (input) {
                is StringValue -> Instant.parse(input.value)
                is IntValue -> Instant.ofEpochMilli(input.value.longValueExact())
                else -> throw CoercingSerializeException("Invalid value '$input' for LocalTime")
            }
    })
)