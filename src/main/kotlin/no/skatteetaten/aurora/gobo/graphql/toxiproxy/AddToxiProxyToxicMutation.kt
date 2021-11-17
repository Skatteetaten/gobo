package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.expediagroup.graphql.server.operations.Mutation
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import org.springframework.stereotype.Component
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicContext
import no.skatteetaten.aurora.gobo.security.ifValidUserToken

// import graphql.schema.DataFetchingEnvironment
// import no.skatteetaten.aurora.gobo.security.checkValidUserToken

@Component
class AddToxiProxyToxicMutation(val toxiProxyToxicService: ToxiProxyToxicService) : Mutation {

    suspend fun addToxiProxyToxic(
        input: AddToxiProxyToxicsInput,
        dfe: DataFetchingEnvironment
    ): String {
        dfe.ifValidUserToken {
            toxiProxyToxicService.addToxiProxyToxic(
                toxiProxyToxicCtx = ToxiProxyToxicContext(
                    token = dfe.token(),
                    affiliationName = input.affiliation,
                    environmentName = input.environment,
                    applicationName = input.application,
                ),
                toxic = input.toxic
            )
        }
        return ""
    }
//
//     private fun List<DeploymentResource>?.toDeploymentEnvironmentResponse() =
//         this?.map {
//             Deployment(
//                 deploymentRef = DeploymentRef(
//                     it.deploymentRef.cluster,
//                     it.deploymentRef.cluster,
//                     it.deploymentRef.affiliation,
//                     it.deploymentRef.environment,
//                     it.deploymentRef.application
//                 ),
//                 deployId = it.deployId,
//                 timestamp = it.timestamp.toInstant(),
//                 message = it.message
//             )
//         }
// }
}

data class AddToxiProxyToxicsInput(
    val affiliation: String,
    val environment: String,
    val application: String,
    val toxic: AddToxiProxyInput,
)

data class AddToxiProxyInput(val name: String, val listen: String, val upstream: String, val enabled: Boolean, val toxics: List<AddToxicInput>)

data class AddToxicInput(
    val toxicName: String,
    val type: String,
    val stream: String,
    val toxicity: Int,
    val attributes: List<AddToxicAttributeInput>
)

data class AddToxicAttributeInput(val key: String, val value: String)
