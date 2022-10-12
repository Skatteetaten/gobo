package no.skatteetaten.aurora.gobo.graphql.loadtest

import java.io.File
import org.junit.jupiter.api.Test
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.AddOrUpdateToxiProxyInput
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

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

        // testJsonDeserialize()
        val queryContent = getFileQueryContent("src/gatling/resources/mokey_add_delay_toxic_mutation_ENDRET.json")
        println(queryContent)

        val input = deserializeAddOrUpdateToxiProxyInput(queryContent.variables)
    }

    private fun getFileQueryContent(jsonFilename: String): GoboLoadtestSimulationQueryTest.QueryContent {
        val queryContent: GoboLoadtestSimulationQueryTest.QueryContent = jacksonObjectMapper().readValue(File(jsonFilename))
        return queryContent
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

        val jsonTest = """{"affiliation":"aup","environment":"utv01","application":"m78879-gobo9.2","toxiProxy":{name: "mokeyToxic",""fghfg}"""
        var queryContent = jacksonObjectMapper().readValue<AddOrUpdateToxiProxyInput>(jsonTest)
        println(queryContent)
    }
}
