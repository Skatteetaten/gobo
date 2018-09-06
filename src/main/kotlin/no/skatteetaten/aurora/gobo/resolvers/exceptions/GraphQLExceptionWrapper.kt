package no.skatteetaten.aurora.gobo.resolvers.exceptions

import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.language.SourceLocation

class GraphQLExceptionWrapper(handlerParameters: DataFetcherExceptionHandlerParameters) : GraphQLError {
    private val exception = handlerParameters.exception as ResolverException
    private val cause = handlerParameters.exception.cause
    private val message = handlerParameters.exception.message
    private val locations = handlerParameters.field.sourceLocation
    private val executionPath = handlerParameters.path

    override fun getExtensions(): MutableMap<String, Any?> =
            mutableMapOf(
                    "code" to exception.code,
                    "cause" to cause?.javaClass?.simpleName,
                    "errorMessage" to exception.errorMessage
            )

    override fun getMessage(): String = message ?: ""

    override fun getErrorType(): ErrorType = ErrorType.DataFetchingException

    override fun getLocations(): MutableList<SourceLocation> = mutableListOf(locations)

    override fun getPath(): MutableList<Any> = executionPath.toList()
}