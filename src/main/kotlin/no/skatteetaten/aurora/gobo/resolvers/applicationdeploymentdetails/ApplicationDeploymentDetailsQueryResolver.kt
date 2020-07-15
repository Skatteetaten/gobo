package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.load
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsQueryResolver : Query {

    suspend fun applicationDeploymentDetails(id: String, dfe: DataFetchingEnvironment) =
        dfe.load<String, ApplicationDeploymentDetails>(id)
}
