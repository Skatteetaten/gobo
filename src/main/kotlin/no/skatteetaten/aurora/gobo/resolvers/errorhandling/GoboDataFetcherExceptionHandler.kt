package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException

private val logger = KotlinLogging.logger { }

class GoboDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun accept(handlerParameters: DataFetcherExceptionHandlerParameters?) {
        handlerParameters ?: return

        if (handlerParameters.exception is GoboException) {
            handlerParameters.executionContext?.addError(GraphQLExceptionWrapper(handlerParameters))
        }

        logger.error { "Exception in data fetcher: ${handlerParameters.exception.message}" }
    }
}