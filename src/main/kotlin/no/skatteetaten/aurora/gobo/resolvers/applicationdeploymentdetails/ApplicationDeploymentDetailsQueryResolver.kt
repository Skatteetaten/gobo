package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.loader
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsQueryResolver : GraphQLQueryResolver {

    fun applicationDeploymentDetails(id: String, dfe: DataFetchingEnvironment) =
        dfe.loader(ApplicationDeploymentDetailsDataLoader::class).load(id)
}
