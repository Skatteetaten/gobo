package no.skatteetaten.aurora.gobo.graphql.errorhandling

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isInstanceOf
import graphql.ExceptionWhileDataFetching
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.klientid
import no.skatteetaten.aurora.gobo.graphql.korrelasjonsid
import no.skatteetaten.aurora.gobo.graphql.query
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletionException
import no.skatteetaten.aurora.gobo.graphql.startTime

class GoboDataFetcherExceptionHandlerTest {
    private val exceptionHandler = GoboDataFetcherExceptionHandler("http://boober", false)
    private val env = mockk<DataFetchingEnvironment>(relaxed = true) {
        every { korrelasjonsid } returns "abc123"
        every { klientid } returns "test-client"
        every { query } returns "{ }"
        every { startTime } returns System.currentTimeMillis()
    }

    @Test
    fun `Given GoboException add GraphQL error to execution context`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(GoboException("test exception"))
            .build()

        val exceptions = exceptionHandler.handleException(handlerParameters)
        assertThat(exceptions.get().errors.size).equals(1)
        assertThat(exceptions.get().errors.first()).isInstanceOf(GraphQLExceptionWrapper::class)
    }

    @Test
    fun `Given wrapped IntegrationDisabledException, add original exception to execution context`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(CompletionException(IntegrationDisabledException("test exception")))
            .build()

        val exceptions = exceptionHandler.handleException(handlerParameters)
        val first = exceptions.get().errors.first() as ExceptionWhileDataFetching
        assertThat(first.exception).isInstanceOf(IntegrationDisabledException::class)
    }

    @Test
    fun `Given Exception adds GraphQL error`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(Exception("test exception"))
            .build()

        val exceptions = exceptionHandler.handleException(handlerParameters)
        assertThat(exceptions.get().errors.size).equals(1)
        assertThat(exceptions.get().errors.first()).isInstanceOf(GraphQLExceptionWrapper::class)
    }

    @Test
    fun `Given SourceSystemException add GraphQL error`() {
        val handlerParameters = DataFetcherExceptionHandlerParameters
            .newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(SourceSystemException(message = "test exception", sourceSystem = ServiceTypes.MOKEY))
            .build()

        val exceptions = exceptionHandler.onException(handlerParameters)
        assertThat(exceptions.errors).hasSize(1)
        assertThat(exceptions.errors.first()).isInstanceOf(GraphQLExceptionWrapper::class)
    }
}
