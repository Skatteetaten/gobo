package no.skatteetaten.aurora.gobo.graphql.errorhandling

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isInstanceOf
import graphql.ExceptionWhileDataFetching
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.schema.DataFetchingEnvironment
import io.mockk.mockk
import java.util.concurrent.CompletionException
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import org.junit.jupiter.api.Test

class GoboDataFetcherExceptionHandlerTest {
    private val exceptionHandler = GoboDataFetcherExceptionHandler()
    private val env = mockk<DataFetchingEnvironment>(relaxed = true)

    @Test
    fun `Given GoboException add GraphQL error to execution context`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(GoboException("test exception"))
            .build()

        val exceptions = exceptionHandler.onException(handlerParameters)
        assertThat(exceptions.errors).hasSize(1)
        assertThat(exceptions.errors.first()).isInstanceOf(GraphQLExceptionWrapper::class)
    }

    @Test
    fun `Given wrapped IntegrationDisabledException, add original exception to execution context`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(CompletionException(IntegrationDisabledException("test exception")))
            .build()

        val exceptions = exceptionHandler.onException(handlerParameters)
        val first = exceptions.errors.first() as ExceptionWhileDataFetching
        assertThat(first.exception).isInstanceOf(IntegrationDisabledException::class)
    }

    @Test
    fun `Given Exception do not add GraphQL error`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(Exception("test exception"))
            .build()

        val exceptions = exceptionHandler.onException(handlerParameters)
        assertThat(exceptions.errors).hasSize(1)
        assertThat(exceptions.errors.first()).isInstanceOf(graphql.ExceptionWhileDataFetching::class)
    }

    @Test
    fun `Given SourceSystemException add GraphQL error`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(SourceSystemException(message = "test exception", sourceSystem = "unit-test"))
            .build()

        val exceptions = exceptionHandler.onException(handlerParameters)
        assertThat(exceptions.errors).hasSize(1)
        assertThat(exceptions.errors.first()).isInstanceOf(GraphQLExceptionWrapper::class)
    }
}
