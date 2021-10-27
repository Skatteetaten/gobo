package no.skatteetaten.aurora.gobo.graphql.scalars

import graphql.language.IntValue
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingSerializeException
import java.math.BigInteger

object KotlinLongScalar : Coercing<Long, BigInteger> {
    override fun serialize(input: Any): BigInteger = BigInteger(input.toString())

    override fun parseValue(input: Any): Long = parseLiteral(input)

    override fun parseLiteral(input: Any): Long = when (input) {
        is StringValue -> input.value.toLong()
        is IntValue -> input.value.longValueExact()
        else -> throw CoercingSerializeException("Invalid value '$input' for kotlin.Long")
    }
}
