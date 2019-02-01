package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import assertk.assertThat
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
        assertThat(exceptionWrapper.message).isEqualTo("test exception")
        assertThat(exceptionWrapper.locations[0]).isEqualTo(sourceLocation)
        assertThat(exceptionWrapper.path).isEmpty()
        assertThat(exceptionWrapper.extensions["code"]).isEqualTo("INTERNAL_SERVER_ERROR")
        assertThat(exceptionWrapper.extensions["cause"]).isEqualTo(IllegalStateException::class.simpleName)
        assertThat(exceptionWrapper.extensions["errorMessage"]).isEqualTo("error message")
        assertThat(exceptionWrapper.extensions["sourceSystem"]).isNull()
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
        assertThat(exceptionWrapper.message).isEqualTo("test exception")
        assertThat(exceptionWrapper.extensions["code"]).isEqualTo("INTERNAL_SERVER_ERROR")
        assertThat(exceptionWrapper.extensions["cause"]).isEqualTo(IllegalStateException::class.simpleName)
        assertThat(exceptionWrapper.extensions["errorMessage"]).isEqualTo("error message")
        assertThat(exceptionWrapper.extensions["sourceSystem"]).isEqualTo("source")
    }
}
