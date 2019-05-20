package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ExecutionPath
import graphql.language.SourceLocation
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException

class GraphQLExceptionWrapper private constructor(
    private val exception: Throwable,
    private val message: String? = exception.message,
    private val cause: Throwable? = exception.cause,
    private val location: SourceLocation? = null,
    private val executionPath: ExecutionPath? = null
) : GraphQLError {
    constructor(handlerParameters: DataFetcherExceptionHandlerParameters) : this(
        exception = handlerParameters.exception,
        location = handlerParameters.sourceLocation,
        executionPath = handlerParameters.path
    )

    constructor(exceptionWhileDataFetching: ExceptionWhileDataFetching) : this(
        exception = exceptionWhileDataFetching.exception,
        executionPath = exceptionWhileDataFetching.path?.let { ExecutionPath.fromList(it) }
    )

    override fun getExtensions(): Map<String, Any?> {
        val goboException = exception as GoboException
        return mapOf<String, Any?>(
            "code" to goboException.code,
            "cause" to cause?.javaClass?.simpleName,
            "errorMessage" to goboException.errorMessage,
            "sourceSystem" to if (exception is SourceSystemException) exception.sourceSystem else null
        ).filter { it.value != null }
    }

    override fun getMessage(): String = message ?: ""

    override fun getErrorType(): ErrorType = ErrorType.DataFetchingException

    override fun getLocations() = location?.let { mutableListOf(it) } ?: mutableListOf()

    override fun getPath() = executionPath?.toList()?.toMutableList() ?: mutableListOf()
}
