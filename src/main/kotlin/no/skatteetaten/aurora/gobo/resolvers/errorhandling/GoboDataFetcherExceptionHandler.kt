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

        if (handlerParameters.exception is GoboException) {
            handlerParameters.executionContext?.addError(GraphQLExceptionWrapper(handlerParameters))
        }

        val exceptionMessage = if (handlerParameters.exception is SourceSystemException) {
            val exception = handlerParameters.exception as SourceSystemException
            "Exception in data fetcher, exception=\"${exception.message}\" - source=${exception.sourceSystem}"
        } else {
            "Exception in data fetcher, exception=\"${handlerParameters.exception.message}\""
        }

        logger.error { exceptionMessage }
    }
}