package no.skatteetaten.aurora.gobo.graphql.errorhandling

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import graphql.ExceptionWhileDataFetching
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ResultPath
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.graphql.AccessDeniedException
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.junit.jupiter.api.Test

class GraphQLExceptionWrapperTest {
    private val paramsBuilder = DataFetcherExceptionHandlerParameters
        .newExceptionParameters()
        .dataFetchingEnvironment(
            mockk(relaxed = true) {
                every { getContext<GoboGraphQLContext>() } returns mockk(relaxed = true)
            }
        )

    @Test
    fun `Create new GraphQLExceptionWrapper`() {
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
        assertThat(exceptionWrapper.locations[0]).isNotNull()
        assertThat(exceptionWrapper.path).isEmpty()
        assertThat(exceptionWrapper.extensions["errorMessage"]).isEqualTo("error message")
    }

    @Test
    fun `Create new GraphQLExceptionWrapper with source system`() {
        val handlerParams = paramsBuilder.exception(
            SourceSystemException(
                message = "test exception",
                cause = IllegalStateException(),
                code = "INTERNAL_SERVER_ERROR",
                errorMessage = "error message",
                sourceSystem = ServiceTypes.MOKEY
            )
        ).build()

        val exceptionWrapper = GraphQLExceptionWrapper(handlerParams)
        assertThat(exceptionWrapper.message).isEqualTo("test exception")
        assertThat(exceptionWrapper.extensions["errorMessage"]).isEqualTo("error message")
    }

    @Test
    fun `Create new GraphQLExceptionWrapper with ExceptionWhileDataFetching`() {
        val exceptionWhileDataFetching =
            ExceptionWhileDataFetching(ResultPath.rootPath(), AccessDeniedException("test exception"), null)

        val exceptionWrapper = GraphQLExceptionWrapper(exceptionWhileDataFetching)
        assertThat(exceptionWrapper.message).isEqualTo("test exception")
        assertThat(exceptionWrapper.extensions["errorMessage"]).isEqualTo("test exception")
    }
}
