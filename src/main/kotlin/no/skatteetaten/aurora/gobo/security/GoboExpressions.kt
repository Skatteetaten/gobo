package no.skatteetaten.aurora.gobo.security

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.springboot.AuroraAuthenticationToken
import no.skatteetaten.aurora.springboot.anonymousAuthenticationToken
import org.springframework.stereotype.Component

// TODO wait until SpEL supports reactive: @PreAuthorize("@goboExpressions.isNotAnonymousUser(#dfe)")
//  @PreAuthorize("hasAuthority(@environment.getProperty('credentials.registerPostgres.allowedAdGroup'))")
// https://github.com/spring-projects/spring-security/issues/9401
@Component
class GoboExpressions {

    fun isAnonymousUser(dfe: DataFetchingEnvironment) =
        dfe.getContext<GoboGraphQLContext>().securityContext.map {
            (it.authentication as AuroraAuthenticationToken) == anonymousAuthenticationToken
        }

    fun isNotAnonymousUser(dfe: DataFetchingEnvironment) =
        dfe.getContext<GoboGraphQLContext>().securityContext.map {
            (it.authentication as AuroraAuthenticationToken) != anonymousAuthenticationToken
        }
}
