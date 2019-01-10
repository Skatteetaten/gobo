package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import assertk.assert
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ExecutionPath
import graphql.language.Field
import graphql.language.SourceLocation
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.junit.jupiter.api.Test

class GraphQLExceptionWrapperTest {
    private val sourceLocation = SourceLocation(0, 0)
    private val paramsBuilder = DataFetcherExceptionHandlerParameters
        .newExceptionParameters()
        .field(Field.newField().sourceLocation(sourceLocation).name("name").build())
        .path(ExecutionPath.parse(""))

    @Test
    fun `Create new GrapQLExceptionWrapper`() {
        val handlerParams = paramsBuilder.exception(
            GoboException(
                message = "test exception",
                cause = IllegalStateException(),
                code = "INTERNAL_SERVER_ERROR",
                errorMessage = "error message"
            )
        ).build()

        val exceptionWrapper = GraphQLExceptionWrapper(handlerParams)
        assert(exceptionWrapper.message).isEqualTo("test exception")
        assert(exceptionWrapper.locations[0]).isEqualTo(sourceLocation)
        assert(exceptionWrapper.path).isEmpty()
        assert(exceptionWrapper.extensions["code"]).isEqualTo("INTERNAL_SERVER_ERROR")
        assert(exceptionWrapper.extensions["cause"]).isEqualTo(IllegalStateException::class.simpleName)
        assert(exceptionWrapper.extensions["errorMessage"]).isEqualTo("error message")
        assert(exceptionWrapper.extensions["sourceSystem"]).isNull()
    }

    @Test
    fun `Create new GrapQLExceptionWrapper with source system`() {
        val handlerParams = paramsBuilder.exception(
            SourceSystemException(
                message = "test exception",
                cause = IllegalStateException(),
                code = "INTERNAL_SERVER_ERROR",
                errorMessage = "error message",
                sourceSystem = "source"
            )
        ).build()

        val exceptionWrapper = GraphQLExceptionWrapper(handlerParams)
        assert(exceptionWrapper.message).isEqualTo("test exception")
        assert(exceptionWrapper.extensions["code"]).isEqualTo("INTERNAL_SERVER_ERROR")
        assert(exceptionWrapper.extensions["cause"]).isEqualTo(IllegalStateException::class.simpleName)
        assert(exceptionWrapper.extensions["errorMessage"]).isEqualTo("error message")
        assert(exceptionWrapper.extensions["sourceSystem"]).isEqualTo("source")
    }
}
