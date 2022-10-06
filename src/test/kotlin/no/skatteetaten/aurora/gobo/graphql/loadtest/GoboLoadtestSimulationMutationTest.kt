package no.skatteetaten.aurora.gobo.graphql.loadtest

import java.io.File
import org.junit.jupiter.api.Test
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.AddOrUpdateToxiProxyInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.DeleteToxiProxyToxicsInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.ToxiProxyInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.ToxicAttributeInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.ToxicInput
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

const val TOXY_PROXY_NAME = "test_toxiProxy"
const val TOXIC_NAME = "latency_downstream"
const val TOXIC_PROXY_LISTEN = "\"[::]:8090\""
const val TOXIC_PROXY_UPSTREAM = "\"0.0.0.0:8080\""
const val TOXIC_PROXY_ENABLED = true

class GoboLoadtestSimulationMutationTest : GraphQLTestWithDbhAndSkap() {

    // @Value("classpath:graphql/mutations/addToxiProxyToxic.graphql")
    // private lateinit var addToxiProxyToxicMutation: Resource
    //
    // @Value("classpath:graphql/mutations/updateToxiProxyToxic.graphql")
    // private lateinit var updateToxiProxyToxicMutation: Resource
    //
    // @Value("classpath:graphql/mutations/updateToxiProxy.graphql")
    // private lateinit var updateToxiProxyMutation: Resource
    //
    // @Value("classpath:graphql/mutations/deleteToxiProxyToxic.graphql")
    // private lateinit var deleteToxiProxyToxicMutation: Resource

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

        testJsonDeserialize()
        // val queryContent = getQueryContent("src/gatling/resources/mokey_add_delay_toxic_mutation_ENDRET.json")
        //
        // // val input = deserializeAddOrUpdateToxiProxyInput(queryContent.variables)
        // val addToxiProxyToxicsInput = getAddOrUpdateToxiProxyToxicsInput()
        //
        // webTestClient.queryGraphQL(queryContent.query, addToxiProxyToxicsInput, "test-token")
        //     .expectStatus().isOk
        //     .expectBody()
        // println()
        // .graphqlDataWithPrefix("addToxiProxyToxic") {
        //     graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
        //     graphqlData("toxicName").isEqualTo(TOXIC_NAME)
        // }
        // .graphqlDoesNotContainErrors()
    }

    // @Test
    // fun `delete toxic with name`() {
    //
    //     val deleteToxiProxyToxicsInput = getDeleteToxiProxyToxicsInput()
    //
    //     webTestClient.queryGraphQL(deleteToxiProxyToxicMutation, deleteToxiProxyToxicsInput, "test-token")
    //         .expectStatus().isOk
    //         .expectBody()
    //         .graphqlDataWithPrefix("deleteToxiProxyToxic") {
    //             graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
    //             graphqlData("toxicName").isEqualTo(TOXIC_NAME)
    //         }
    //         .graphqlDoesNotContainErrors()
    // }

    private fun getAddOrUpdateToxiProxyToxicsInput(): AddOrUpdateToxiProxyInput {
        val attr = listOf(
            ToxicAttributeInput("key", "latency"),
            ToxicAttributeInput("value", "1234")
        )
        val toxics = ToxicInput(
            name = TOXIC_NAME,
            type = "latency",
            stream = "downstream",
            toxicity = 1.0,
            attributes = attr
        )
        val toxiProxyInput = ToxiProxyInput(name = TOXY_PROXY_NAME, toxics = toxics)
        return AddOrUpdateToxiProxyInput(
            affiliation = "test_aff",
            environment = "test_env",
            application = "test_app",
            toxiProxy = toxiProxyInput
        )
    }

    private fun getDeleteToxiProxyToxicsInput() =
        DeleteToxiProxyToxicsInput(
            affiliation = "test_aff",
            environment = "test_env",
            application = "test_app",
            toxiProxyName = TOXY_PROXY_NAME,
            toxicName = TOXIC_NAME
        )

    private fun getQueryContent(jsonFilename: String): GoboLoadtestSimulationQueryTest.QueryContent {
        val jsonFile = File(jsonFilename)
        val deploymentSpec: GoboLoadtestSimulationQueryTest.QueryContent = jacksonObjectMapper().readValue(jsonFile)

        return deploymentSpec
    }

    private fun deserializeAddOrUpdateToxiProxyInput(jsonInput: String): AddOrUpdateToxiProxyInput {
        var input: AddOrUpdateToxiProxyInput = jacksonObjectMapper().readValue(jsonInput)
        return input
    }

    data class Movie(
        var name: String,
        var studio: String,
        var rating: Float? = 1f
    )

    private fun testJsonDeserialize() {
        // val json = """{"name":"Endgame","studio":"Marvel","rating":9.2}"""
        // var movie = jacksonObjectMapper().readValue<Movie>(json)
        // println(movie)

        val jsonTest = """{"affiliation":"aup","environment":"utv01","application":"m78879-gobo9.2","toxiProxy":""fghfg}"""
        var queryContent = jacksonObjectMapper().readValue<AddOrUpdateToxiProxyInput>(jsonTest)
        println(queryContent)
    }
}
