package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException

private val logger = KotlinLogging.logger { }

class GoboDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun accept(handlerParameters: DataFetcherExceptionHandlerParameters?) {
        handlerParameters ?: return

        val exception = handlerParameters.exception
        if (exception is GoboException) {
            handlerParameters.executionContext?.addError(GraphQLExceptionWrapper(handlerParameters))
        }

        val exceptionMessage = if (exception is SourceSystemException) {
            "Exception in data fetcher, exception=\"${exception.message}\" - source=${exception.sourceSystem}"
        } else {
            "Exception in data fetcher, exception=\"${exception.message}\""
        }

        logger.error { exceptionMessage }
    }
}