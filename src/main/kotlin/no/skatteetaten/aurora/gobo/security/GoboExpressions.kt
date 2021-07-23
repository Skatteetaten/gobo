package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class GoboExpressions {
    fun isAnonymous(dfe: DataFetchingEnvironment) = dfe.getContext<GoboGraphQLContext>().isAnonymous()
    fun isNotAnonymous(dfe: DataFetchingEnvironment) = dfe.getContext<GoboGraphQLContext>().isNotAnonymous()
}
