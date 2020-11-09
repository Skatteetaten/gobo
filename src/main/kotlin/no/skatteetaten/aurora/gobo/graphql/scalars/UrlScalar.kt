package no.skatteetaten.aurora.gobo.graphql.scalars

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.net.MalformedURLException
import java.net.URL

object UrlScalar : Coercing<URL, String> {

    override fun serialize(input: Any?) = (input as URL).toString()

    override fun parseValue(input: Any?): URL = parseLiteral(input)

    override fun parseLiteral(input: Any?): URL {
        if (input !is StringValue) throw CoercingSerializeException("Invalid value '$input' for URL")

        return try {
            URL(input.value)
        } catch (e: MalformedURLException) {
            throw CoercingParseValueException("Input string '${input.value}' could not be parsed into a URL", e)
        }
    }
}
