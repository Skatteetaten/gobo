package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import assertk.assert
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ExecutionPath
import graphql.language.Field
import graphql.language.SourceLocation
import no.skatteetaten.aurora.gobo.integration.GoboException
import org.junit.jupiter.api.Test

class GraphQLExceptionWrapperTest {

    @Test
    fun `Create new GrapQLExceptionWrapper`() {
        val sourceLocation = SourceLocation(0, 0)
        val field = Field("name").apply {
            this.sourceLocation = sourceLocation
        }
        val handlerParameters = DataFetcherExceptionHandlerParameters(
                null,
                null,
                field,
                null,
                null,
                ExecutionPath.parse(""),
                GoboException("test exception", IllegalStateException(), "INTERNAL_SERVER_ERROR", "error message"))

        val exceptionWrapper = GraphQLExceptionWrapper(handlerParameters)
        assert(exceptionWrapper.message).isEqualTo("test exception")
        assert(exceptionWrapper.locations[0]).isEqualTo(sourceLocation)
        assert(exceptionWrapper.path).isEmpty()
        assert(exceptionWrapper.extensions["code"]).isEqualTo("INTERNAL_SERVER_ERROR")
        assert(exceptionWrapper.extensions["cause"]).isEqualTo(IllegalStateException::class.simpleName)
        assert(exceptionWrapper.extensions["errorMessage"]).isEqualTo("error message")
    }
}