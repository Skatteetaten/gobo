package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import no.skatteetaten.aurora.gobo.exceptions.GoboException

class GoboDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun accept(handlerParameters: DataFetcherExceptionHandlerParameters?) {
        handlerParameters ?: return

        if (handlerParameters.exception is GoboException) {
            handlerParameters.executionContext?.addError(GraphQLExceptionWrapper(handlerParameters))
        }
    }
}