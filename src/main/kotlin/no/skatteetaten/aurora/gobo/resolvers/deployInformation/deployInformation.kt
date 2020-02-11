package no.skatteetaten.aurora.gobo.resolvers.deployInformation

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class DeployInformationQueryResolver(val applicationDeploymentService: ApplicationDeploymentService) :
    GraphQLQueryResolver {

    fun deployInformation(input: DeployInformationInput, dfe: DataFetchingEnvironment): DeployInformation {
        val token = dfe.currentUser().token
        val spec = applicationDeploymentService.getSpec(token, input.auroraConfigName, input.auroraConfigReference, input.path)

        val infoList = spec.items.map {
            DeployDetail(
                // TODO releaseTo
                cluster = it.at("/cluster/value").textValue(),
                environment = it.at("/envName/value").textValue(),
                application = it.at("/name/value").textValue(),
                version = it.at("/version/value").textValue(),
                replicas = it.at("/replicas/value").intValue().toString(),
                type = it.at("/type/value").textValue(),
                deploy_strategy = it.at("/deployStrategy/type/value").textValue()
            )
        }

        return DeployInformation(infoList)
    }
}

data class DeployInformation(
    val deployDetail: List<DeployDetail>
)

data class DeployDetail(
    val cluster: String,
    val environment: String,
    val application: String,
    val version: String,
    val replicas: String,
    val type: String,
    val deploy_strategy: String
)

data class DeployInformationInput(
    val auroraConfigName: String,
    val auroraConfigReference: String,
    val path: List<String>
)
