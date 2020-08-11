package no.skatteetaten.aurora.gobo.resolvers.scalars

import assertk.assertThat
import assertk.assertions.isEqualTo
import graphql.language.IntValue
import graphql.language.StringValue
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.jupiter.api.Test

class InstantScalarTest {

    private val dateTimeScalar = InstantScalar
    private val instant = LocalDateTime.of(2018, 7, 2, 14, 56).toInstant(ZoneOffset.UTC)

    @Test
    fun `Serialize dateTime`() {
        val serialized = dateTimeScalar.serialize(instant)
        assertThat(serialized).isEqualTo(instant.toString())
    }

    @Test
    fun `Parse StringValue`() {
        val value = dateTimeScalar.parseValue(StringValue(instant.toString()))
        assertThat(value).isEqualTo(instant)
    }

    @Test
    fun `Parse IntValue`() {
        val value = dateTimeScalar.parseValue(IntValue(BigInteger.valueOf(instant.toEpochMilli())))
        assertThat(value).isEqualTo(instant)
    }
}
