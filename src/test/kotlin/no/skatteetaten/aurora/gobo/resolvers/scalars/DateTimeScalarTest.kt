package no.skatteetaten.aurora.gobo.resolvers.scalars

import assertk.assert
import assertk.assertions.isEqualTo
import graphql.language.IntValue
import graphql.language.StringValue
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset

class DateTimeScalarTest {

    private val dateTimeScalar = DateTimeScalar()
    private val instant = LocalDateTime.of(2018, 7, 2, 14, 56).toInstant(ZoneOffset.UTC)

    @Test
    fun `Serialize dateTime`() {
        val serialized = dateTimeScalar.coercing.serialize(instant)
        assert(serialized).isEqualTo(instant.toString())
    }

    @Test
    fun `Parse StringValue`() {
        val value = dateTimeScalar.coercing.parseValue(StringValue(instant.toString()))
        assert(value).isEqualTo(instant)
    }

    @Test
    fun `Parse IntValue`() {
        val value = dateTimeScalar.coercing.parseValue(IntValue(BigInteger.valueOf(instant.toEpochMilli())))
        assert(value).isEqualTo(instant)
    }
}