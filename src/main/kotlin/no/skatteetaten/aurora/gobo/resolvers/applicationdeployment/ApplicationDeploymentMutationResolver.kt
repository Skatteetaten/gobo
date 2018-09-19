package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentMutationResolver : GraphQLMutationResolver {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput): ApplicationDeployment {
        return ApplicationDeployment(
            "id", "name", "affiliationId", "environment",
            "namespaceId", Status("code", "comment"), Version("deployTag", null), null
        )
    }
}