package no.skatteetaten.aurora.gobo.graphql.errorhandling

import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ExecutionPath
import graphql.language.SourceLocation
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.graphql.korrelasjonsid
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.web.reactive.function.client.WebClientResponseException

class GraphQLExceptionWrapper private constructor(
    private val exception: Throwable,
    private val message: String? = if (exception is WebClientResponseException) {
        exception.statusCode.name
    } else {
        exception.message
    },
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

    override fun getExtensions(): Map<String, Any?> =
        when (exception) {
            is GoboException -> {
                mapOf<String, Any?>(
                    "code" to exception.code,
                    "cause" to cause?.javaClass?.simpleName,
                    "errorMessage" to exception.errorMessage,
                    "sourceSystem" to if (exception is SourceSystemException) exception.sourceSystem else null
                ).filter { it.value != null } + exception.extensions
            }
            is WebClientResponseException -> {
                mapOf(
                    "code" to exception.statusCode.name,
                    "cause" to cause?.javaClass?.simpleName,
                    "errorMessage" to exception.responseBodyAsString,
                    AuroraRequestParser.KORRELASJONSID_FIELD to exception.request.korrelasjonsid()
                )
            }
            else -> {
                emptyMap()
            }
        }

    override fun getMessage(): String = message ?: ""

    override fun getErrorType(): ErrorType = ErrorType.DataFetchingException

    override fun getLocations() = location?.let { mutableListOf(it) } ?: mutableListOf()

    override fun getPath() = executionPath?.toList()?.toMutableList() ?: mutableListOf()
}
