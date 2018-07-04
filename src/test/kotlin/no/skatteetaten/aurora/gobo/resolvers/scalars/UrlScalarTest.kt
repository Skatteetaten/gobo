package no.skatteetaten.aurora.gobo.resolvers.scalars

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.catch
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
        assert(serialized).isEqualTo("http://localhost")
    }

    @Test
    fun `Parse StringValue`() {
        val value = urlScalar.coercing.parseValue(StringValue(url.toString()))
        assert(value).isEqualTo(url)
    }

    @Test
    fun `Throw exception when url is not valid`() {
        val exception = catch { urlScalar.coercing.parseValue(StringValue("This is not a valid url")) }
        assert(exception).isNotNull {
            it.message().isEqualTo("Input string 'This is not a valid url' could not be parsed into a URL")
        }
    }

    @Test
    fun `Throw exception when parsing unsupported type`() {
        val input = IntValue(BigInteger.valueOf(123))
        val exception = catch { urlScalar.coercing.parseValue(input) }
        assert(exception).isNotNull {
            it.message().isEqualTo("Invalid value '$input' for URL")
        }
    }
}