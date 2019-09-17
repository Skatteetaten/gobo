package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import graphql.ExceptionWhileDataFetching
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.SourceSystemException

private val logger = KotlinLogging.logger { }

class GoboDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters?): DataFetcherExceptionHandlerResult {
        handlerParameters ?: return DataFetcherExceptionHandlerResult.newResult().build()
        handlerParameters.logErrorInfo()

        val graphqlException = if (handlerParameters.exception is GoboException) {
            GraphQLExceptionWrapper(handlerParameters)
        } else {
            exceptionWhileDataFetching(handlerParameters)
        }
        return DataFetcherExceptionHandlerResult.newResult(graphqlException).build()
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
        val dataSourceValue = jacksonObjectMapper().writeValueAsString(it)
        log("$msg\ndataSource=${it.javaClass.simpleName} dataSourceValue=$dataSourceValue")
    } ?: log(msg)
}

private fun exceptionWhileDataFetching(handlerParameters: DataFetcherExceptionHandlerParameters) =
    ExceptionWhileDataFetching(handlerParameters.path, handlerParameters.exception, handlerParameters.sourceLocation)