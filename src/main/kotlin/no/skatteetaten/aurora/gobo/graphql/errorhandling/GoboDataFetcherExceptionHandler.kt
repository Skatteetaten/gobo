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
import org.apache.commons.lang3.exception.ExceptionUtils

private val logger = KotlinLogging.logger { }

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
        exception.sourceSystem
    } else {
        ""
    }

    val msg =
        "Exception in data fetcher, exception=\"$exception\" cause=\"$cause\" message=\"$exceptionName\" source=\"$source\""

    val log = fun(message: String) {
        if (source.isNullOrEmpty()) logger.error(message)
        else logger.warn(message)
    }

    this.dataFetchingEnvironment?.getSource<Any>()?.let {
        log("$msg\ndataSource=${it.javaClass.simpleName} dataSourceValue=$it")
    } ?: log(msg)
}

private fun DataFetcherExceptionHandlerParameters.toExceptionWhileDataFetching() =
    ExceptionWhileDataFetching(path, exception, sourceLocation)

private fun DataFetcherExceptionHandlerParameters.toExceptionWhileDataFetching(t: Throwable) =
    ExceptionWhileDataFetching(path, t, sourceLocation)
