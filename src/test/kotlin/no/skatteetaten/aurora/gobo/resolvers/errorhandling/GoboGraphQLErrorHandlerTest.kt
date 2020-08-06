package no.skatteetaten.aurora.gobo.resolvers.errorhandling

// FIXME graphql error handler test
/*
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
*/
