package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ExecutionContext
import graphql.language.Field
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.skatteetaten.aurora.gobo.exceptions.GoboException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoboDataFetcherExceptionHandlerTest {
    private val exceptionHandler = GoboDataFetcherExceptionHandler()
    private val executionContext = mockk<ExecutionContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(executionContext)
    }

    @Test
    fun `Given GoboException add GraphQL error to execution context`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters(
                executionContext,
                null,
                Field("name"),
                null,
                null,
                null,
                GoboException("test exception"))
        exceptionHandler.accept(handlerParameters)
        verify { executionContext.addError(any()) }
    }

    @Test
    fun `Given Exception do not add GraphQL error`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters(
                executionContext,
                null,
                Field("name"),
                null,
                null,
                null,
                Exception("test exception"))
        exceptionHandler.accept(handlerParameters)
        verify(exactly = 0) { executionContext.addError(any()) }
    }
}