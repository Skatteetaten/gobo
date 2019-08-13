package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import graphql.ExceptionWhileDataFetching
import graphql.execution.ExecutionPath
import graphql.servlet.core.GenericGraphQLError
import no.skatteetaten.aurora.gobo.GoboException
import org.junit.jupiter.api.Test

class GoboGraphQLErrorHandlerTest {

    private val goboGraphQLErrorHandler = GoboGraphQLErrorHandler()

    @Test
    fun `Process generic error`() {
        val error = GenericGraphQLError("error")
        val processedErrors = goboGraphQLErrorHandler.processErrors(mutableListOf(error))
        assertThat(processedErrors[0]).isSameAs(error)
    }

    @Test
    fun `Process Gobo error`() {
        val error =
            ExceptionWhileDataFetching(ExecutionPath.parse("/test1/test2"), GoboException("gobo exception"), null)
        val processedErrors = goboGraphQLErrorHandler.processErrors(mutableListOf(error))

        assertThat(processedErrors.first().message).isEqualTo("gobo exception")
        assertThat(processedErrors.first().extensions["errorMessage"]).isEqualTo("gobo exception")
        assertThat(processedErrors.first().path[0]).isEqualTo("test1")
        assertThat(processedErrors.first().path[1]).isEqualTo("test2")
    }
}