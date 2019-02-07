package no.skatteetaten.aurora.gobo.resolvers.errorhandling

import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.servlet.GraphQLErrorHandler
import no.skatteetaten.aurora.gobo.GoboException
import org.springframework.stereotype.Component

@Component
class GoboGraphQLErrorHandler : GraphQLErrorHandler {
    override fun processErrors(errors: MutableList<GraphQLError>?): MutableList<GraphQLError> {
        errors ?: return mutableListOf()
        val errorsMap = errors.map {
            if (it is ExceptionWhileDataFetching && it.exception is GoboException) {
                GraphQLExceptionWrapper(it)
            } else {
                it
            }
        }
        return errorsMap.toMutableList()
    }
}