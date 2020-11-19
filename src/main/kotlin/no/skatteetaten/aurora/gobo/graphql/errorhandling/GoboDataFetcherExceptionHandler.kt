package no.skatteetaten.aurora.gobo.graphql.errorhandling

import graphql.ExceptionWhileDataFetching
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.AccessDeniedException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.klientid
import no.skatteetaten.aurora.gobo.graphql.korrelasjonsid
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

private val logger = KotlinLogging.logger { }

@Component
class GoboDataFetcherExceptionHandler(@Value("integrations.boober.url") private val booberUrl: String) :
    DataFetcherExceptionHandler {
    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters?): DataFetcherExceptionHandlerResult {
        handlerParameters ?: return DataFetcherExceptionHandlerResult.newResult().build()

        val graphqlException = handlerParameters.handleIntegrationDisabledException()?.let {
            handlerParameters.toExceptionWhileDataFetching(it)
        } ?: handlerParameters.handleGeneralDataFetcherException(booberUrl)

        return DataFetcherExceptionHandlerResult.newResult(graphqlException).build()
    }
}

private fun DataFetcherExceptionHandlerParameters.handleIntegrationDisabledException() = ExceptionUtils
    .getThrowableList(exception)
    .find { it is IntegrationDisabledException }
    ?.let {
        logger.debug(exception) {
            "Integration is disabled, unable to fetch data"
        }
        it
    }

private fun DataFetcherExceptionHandlerParameters.handleGeneralDataFetcherException(booberUrl: String): GraphQLExceptionWrapper {
    val exception = this.exception
    val exceptionName = this::class.simpleName
    val cause = exception.cause?.let { it::class.simpleName } ?: ""
    val source = if (exception is SourceSystemException) {
        "source=\"${exception.sourceSystem}\""
    } else {
        ""
    }

    val logText =
        "Exception while fetching data, exception=\"$exception\" cause=\"$cause\" message=\"$exceptionName\" path=\"$path\" $source ${exception.logTextRequest()}"
    if (exception.isWebClientResponseWarnLoggable(booberUrl) || exception.isAccessDenied()) {
        logger.warn(logText)
    } else {
        logger.error(logText)
    }

    if (exception.isLoggableException()) {
        logger.error(exception) { "Data fetching failed with loggable exception" }
    }

    return GraphQLExceptionWrapper(this)
}

private fun Throwable.isWebClientResponseWarnLoggable(booberUrl: String) = this is WebClientResponseException && (
    isForbidden() || isNotFound() || isBooberBadRequest(booberUrl)
    )

private fun Throwable.logTextRequest() = if (this is WebClientResponseException) {
    val korrelasjonsId = request.korrelasjonsid()?.let { "Korrelasjonsid=\"$it\"" } ?: ""
    val clientId = request.klientid()?.let { "Klientid=\"$it\"" } ?: ""
    val referer = request?.headers?.getFirst(HttpHeaders.REFERER)?.let { "Referer=\"$it\"" } ?: ""
    "$korrelasjonsId $clientId $referer statusCode=\"$statusCode\" " +
        "statusText=\"$statusText\" responseBody=\"$responseBodyAsString\""
} else {
    ""
}

private fun WebClientResponseException.isForbidden() = statusCode == HttpStatus.FORBIDDEN
private fun WebClientResponseException.isNotFound() = statusCode == HttpStatus.NOT_FOUND

private fun WebClientResponseException.isBooberBadRequest(booberUrl: String) = statusCode == HttpStatus.BAD_REQUEST &&
    request?.uri?.toASCIIString()?.startsWith(booberUrl) ?: false

private fun Throwable.isAccessDenied() = this is AccessDeniedException
private fun Throwable.isLoggableException() = this is ClassCastException

private fun DataFetcherExceptionHandlerParameters.toExceptionWhileDataFetching(t: Throwable) =
    ExceptionWhileDataFetching(path, t, sourceLocation)
