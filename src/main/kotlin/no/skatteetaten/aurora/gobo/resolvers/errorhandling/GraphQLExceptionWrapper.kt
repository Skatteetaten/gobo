package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.language.SourceLocation
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException

class GraphQLExceptionWrapper(handlerParameters: DataFetcherExceptionHandlerParameters) : GraphQLError {
    private val exception = handlerParameters.exception as GoboException
    private val cause = handlerParameters.exception.cause
    private val message = handlerParameters.exception.message
    private val locations = handlerParameters.field.sourceLocation
    private val executionPath = handlerParameters.path

    override fun getExtensions() =
        mapOf<String, Any?>(
            "code" to exception.code,
            "cause" to cause?.javaClass?.simpleName,
            "errorMessage" to exception.errorMessage,
            "sourceSystem" to if (exception is SourceSystemException) exception.sourceSystem else null
        ).filter { it.value != null }

    override fun getMessage(): String = message ?: ""

    override fun getErrorType(): ErrorType = ErrorType.DataFetchingException

    override fun getLocations(): MutableList<SourceLocation> = mutableListOf(locations)

    override fun getPath(): MutableList<Any> = executionPath.toList()
}