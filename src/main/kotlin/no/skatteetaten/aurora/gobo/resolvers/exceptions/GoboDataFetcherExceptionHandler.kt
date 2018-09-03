package no.skatteetaten.aurora.gobo.resolvers.exceptions

import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters

class GoboDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun accept(handlerParameters: DataFetcherExceptionHandlerParameters?) {
        handlerParameters ?: return

        if (handlerParameters.exception is ResolverException) {
            handlerParameters.executionContext?.addError(GraphQLExceptionWrapper(handlerParameters))
        }
    }
}