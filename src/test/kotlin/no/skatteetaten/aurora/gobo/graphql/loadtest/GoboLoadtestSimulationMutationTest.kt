package no.skatteetaten.aurora.gobo.graphql.loadtest

import java.io.File
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import com.fasterxml.jackson.module.kotlin.convertValue
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.AddOrUpdateToxiProxyInput
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Import(AddOrUpdateToxiProxyInput::class)
class GoboLoadtestSimulationMutationTest : GraphQLTestWithDbhAndSkap() {

    @MockkBean
    private lateinit var kubernetesCoroutinesClient: KubernetesCoroutinesClient

    @MockkBean(relaxed = true)
    private lateinit var toxiProxyService: ToxiProxyToxicService

    data class QueryContent(
        var operationName: String?,
        var variables: String,
        var query: String
    )

    @Test
    fun `add delay toxic on toxi-proxy`() {

        val queryValue = getQueryValue("src/gatling/resources/mokey_add_delay_toxic_mutation.json")
        val addToxiProxyToxicsInput = deserializeAddOrUpdateToxiProxyInput("src/gatling/resources/mokey_add_delay_toxic_mutation.json")

        webTestClient.queryGraphQL(queryValue, addToxiProxyToxicsInput, "test-token")
            .expectStatus().isOk
            .expectBody()
            .printResult()

        // .graphqlDataWithPrefix("addToxiProxyToxic") {
        //     graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
        //     graphqlData("toxicName").isEqualTo(TOXIC_NAME)
        // }
        // .graphqlDoesNotContainErrors()
        //      val input = deserializeAddOrUpdateToxiProxyInput(queryContent.variables)
    }

    private fun deserializeAddOrUpdateToxiProxyInput(jsonFilename: String): AddOrUpdateToxiProxyInput {
        val fileContentAsJson = jacksonObjectMapper().readTree(File(jsonFilename))
        val toxiProxyInput = jacksonObjectMapper().convertValue<AddOrUpdateToxiProxyInput>(fileContentAsJson.at("/variables/input"))
        return toxiProxyInput
    }

    private fun getQueryValue(jsonFilename: String): String {
        val fileContentAsJson = jacksonObjectMapper().readTree(File(jsonFilename))
        return fileContentAsJson.at("/query").asText()
    }
}
