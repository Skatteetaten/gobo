package no.skatteetaten.aurora.gobo.resolvers.scalars

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.message
import graphql.language.IntValue
import graphql.language.StringValue
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.net.URL

class UrlScalarTest {
    private val urlScalar = UrlScalar()
    private val url = URL("http://localhost")

    @Test
    fun `Serialize url`() {
        val serialized = urlScalar.coercing.serialize(url)
        assertThat(serialized).isEqualTo("http://localhost")
    }

    @Test
    fun `Parse StringValue`() {
        val value = urlScalar.coercing.parseValue(StringValue(url.toString()))
        assertThat(value).isEqualTo(url)
    }

    @Test
    fun `Throw exception when url is not valid`() {
        assertThat {
            urlScalar.coercing.parseValue(StringValue("This is not a valid url"))
        }.isNotNull().isFailure().message()
            .isEqualTo("Input string 'This is not a valid url' could not be parsed into a URL")
    }

    @Test
    fun `Throw exception when parsing unsupported type`() {
        val input = IntValue(BigInteger.valueOf(123))
        assertThat {
            urlScalar.coercing.parseValue(input)
        }.isNotNull().isFailure().message()
            .isEqualTo("Invalid value '$input' for URL")
    }
}