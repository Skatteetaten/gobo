package no.skatteetaten.aurora.gobo.graphql.errorhandling

import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ResultPath
import graphql.language.SourceLocation
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.web.reactive.function.client.WebClientResponseException

class GraphQLExceptionWrapper private constructor(
    val exception: Throwable,
    private val message: String? = if (exception is WebClientResponseException) {
        "Downstream request failed with ${exception.statusCode.name}"
    } else {
        exception.message
    },
    private val cause: Throwable? = exception.cause,
    private val location: SourceLocation? = null,
    private val resultPath: ResultPath? = null,
    private val korrelasjonsId: String? = null
) : GraphQLError {
    constructor(handlerParameters: DataFetcherExceptionHandlerParameters) : this(
        exception = handlerParameters.exception,
        location = handlerParameters.sourceLocation,
        resultPath = handlerParameters.path,
        korrelasjonsId = handlerParameters.dataFetchingEnvironment.getContext<GoboGraphQLContext>()?.korrelasjonsid()
    )

    constructor(exceptionWhileDataFetching: ExceptionWhileDataFetching) : this(
        exception = exceptionWhileDataFetching.exception,
        resultPath = exceptionWhileDataFetching.path?.let { ResultPath.fromList(it) }
    )

    constructor(exception: Throwable) : this(exception = exception, resultPath = null)

    override fun getExtensions(): Map<String, Any?> =
        when (exception) {
            is GoboException -> {
                mapOf<String, Any?>(
                    "errorMessage" to exception.errorMessage,
                    AuroraRequestParser.KORRELASJONSID_FIELD to korrelasjonsId
                ).filter { it.value != null } + exception.extensions
            }
            is WebClientResponseException -> {
                mapOf(
                    "code" to exception.statusCode.name,
                    "cause" to cause?.javaClass?.simpleName,
                    "errorMessage" to exception.responseBodyAsString,
                    AuroraRequestParser.KORRELASJONSID_FIELD to korrelasjonsId
                )
            }
            else -> {
                emptyMap()
            }
        }

    override fun getMessage(): String = message ?: ""

    override fun getErrorType(): ErrorType = ErrorType.DataFetchingException

    override fun getLocations() = location?.let { mutableListOf(it) } ?: mutableListOf()

    override fun getPath() = resultPath?.toList()?.toMutableList() ?: mutableListOf()
}
