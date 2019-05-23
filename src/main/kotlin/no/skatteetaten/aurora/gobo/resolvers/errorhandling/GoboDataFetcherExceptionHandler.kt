package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.ExceptionWhileDataFetching
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException

private val logger = KotlinLogging.logger { }

class GoboDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters?): DataFetcherExceptionHandlerResult {
        handlerParameters ?: return DataFetcherExceptionHandlerResult.newResult().build()

        val exception = handlerParameters.exception
        exception.logExceptionInfo()

        val graphqlException = if (exception is GoboException) {
            GraphQLExceptionWrapper(handlerParameters)
        } else {
            exceptionWhileDataFetching(handlerParameters)
        }
        return DataFetcherExceptionHandlerResult.newResult(graphqlException).build()
    }
}

private fun Throwable.logExceptionInfo() {
        val exceptionMessage = if (this is SourceSystemException) {
            "Exception in data fetcher, exception=\"$message\" - source=$sourceSystem"
        } else {
            "Exception in data fetcher, exception=\"$message\""
        }

        logger.error { exceptionMessage }
}

private fun exceptionWhileDataFetching(handlerParameters: DataFetcherExceptionHandlerParameters) =
    ExceptionWhileDataFetching(handlerParameters.path, handlerParameters.exception, handlerParameters.sourceLocation)