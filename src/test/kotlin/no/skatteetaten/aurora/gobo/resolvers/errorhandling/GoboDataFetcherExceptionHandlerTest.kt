package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isInstanceOf
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.schema.DataFetchingEnvironment
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.GoboException
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
}