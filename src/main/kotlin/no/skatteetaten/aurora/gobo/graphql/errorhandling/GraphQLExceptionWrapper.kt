package no.skatteetaten.aurora.gobo.graphql.errorhandling

import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ResultPath
import graphql.language.SourceLocation
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.graphql.korrelasjonsid
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.reactive.function.client.WebClientResponseException

private val logger = KotlinLogging.logger { }

class GraphQLExceptionWrapper private constructor(
    val exception: Throwable,
    private val message: String? = when (exception) {
        is WebClientResponseException -> "Downstream request failed with ${exception.statusCode.name}"
        is AccessDeniedException -> "Access denied, missing/invalid token or the token does not have the required permissions".also { logger.debug(exception) { "Access denied from Spring Security" } }
        else -> exception.message
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
        korrelasjonsId = handlerParameters.dataFetchingEnvironment.korrelasjonsid
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
