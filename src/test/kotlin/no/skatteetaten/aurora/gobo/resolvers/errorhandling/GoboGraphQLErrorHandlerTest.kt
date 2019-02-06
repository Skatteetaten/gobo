package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isSameAs
import graphql.ExceptionWhileDataFetching
import graphql.execution.ExecutionPath
import graphql.servlet.GenericGraphQLError
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
        val error = ExceptionWhileDataFetching(ExecutionPath.rootPath(), GoboException("gobo exception"), null)
        val processedErrors = goboGraphQLErrorHandler.processErrors(mutableListOf(error))
        assertThat(processedErrors[0].message).contains("gobo exception")
    }
}