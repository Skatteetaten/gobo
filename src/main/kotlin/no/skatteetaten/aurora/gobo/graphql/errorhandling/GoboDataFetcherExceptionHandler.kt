package no.skatteetaten.aurora.gobo.graphql.errorhandling

import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.clientId
import no.skatteetaten.aurora.gobo.graphql.korrelasjonsId
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

private val logger = KotlinLogging.logger { }

@Component
class GoboDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters?): DataFetcherExceptionHandlerResult {
        handlerParameters ?: return DataFetcherExceptionHandlerResult.newResult().build()

        val graphqlException = handlerParameters.handleIntegrationDisabledException()?.let {
            handlerParameters.toExceptionWhileDataFetching(it)
        } ?: handlerParameters.handleGeneralDataFetcherException()

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

private fun DataFetcherExceptionHandlerParameters.handleGeneralDataFetcherException(): GraphQLError {
    logErrorInfo()

    return if (exception is GoboException) {
        GraphQLExceptionWrapper(this)
    } else {
        toExceptionWhileDataFetching()
    }
}

private fun DataFetcherExceptionHandlerParameters.logErrorInfo() {
    val exception = this.exception
    val exceptionName = this::class.simpleName
    val cause = exception.cause?.let { it::class.simpleName } ?: ""
    val source = if (exception is SourceSystemException) {
        "source=\"${exception.sourceSystem}\""
    } else {
        ""
    }

    val status = if (exception is WebClientResponseException) {
        val request = exception.request
        val korrelasjonsId = request.korrelasjonsId()?.let { "Korrelasjonsid=\"$it\"" } ?: ""
        val clientId = request.clientId()?.let { "Klientid=\"$it\"" } ?: ""
        "$korrelasjonsId $clientId statusCode=\"${exception.statusCode} " +
            "statusText=\"${exception.statusText}\" responseBody=\"${exception.responseBodyAsString}\""
    } else {
        ""
    }

    logger.error("Exception in data fetcher, exception=\"$exception\" cause=\"$cause\" message=\"$exceptionName\" $source $status")
}

private fun DataFetcherExceptionHandlerParameters.toExceptionWhileDataFetching() =
    ExceptionWhileDataFetching(path, exception, sourceLocation)

private fun DataFetcherExceptionHandlerParameters.toExceptionWhileDataFetching(t: Throwable) =
    ExceptionWhileDataFetching(path, t, sourceLocation)
