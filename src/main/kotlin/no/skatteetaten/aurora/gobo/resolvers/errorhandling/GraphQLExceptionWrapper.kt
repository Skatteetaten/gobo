package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.language.SourceLocation
import no.skatteetaten.aurora.gobo.GoboException

class GraphQLExceptionWrapper(handlerParameters: DataFetcherExceptionHandlerParameters) : GraphQLError {
    private val exception = handlerParameters.exception as GoboException
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