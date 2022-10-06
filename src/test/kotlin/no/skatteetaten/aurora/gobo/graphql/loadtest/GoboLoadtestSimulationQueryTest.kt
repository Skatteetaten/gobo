package no.skatteetaten.aurora.gobo.graphql.loadtest

import java.io.File
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.cname.CnameAzureDataLoader
import no.skatteetaten.aurora.gobo.graphql.cname.CnameInfoDataLoader
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchemaDataLoader
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.graphql.storagegrid.StorageGridObjectAreaDataLoader
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealStateDataLoader
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.StorageGridObjectAreasService
import no.skatteetaten.aurora.gobo.integration.skap.WebsealService
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService
import no.skatteetaten.aurora.gobo.service.AffiliationService
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import okhttp3.mockwebserver.MockWebServer

@Import(
    AffiliationQuery::class,
    WebsealAffiliationService::class,
    WebsealStateDataLoader::class,
    DatabaseSchemaDataLoader::class,
    StorageGridObjectAreaDataLoader::class,
    CnameAzureDataLoader::class,
    CnameInfoDataLoader::class,
    // GoboLoadtestSimulationQueryTest.TestConfig::class
)
class GoboLoadtestSimulationQueryTest : GraphQLTestWithDbhAndSkap() {

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @MockkBean
    private lateinit var websealService: WebsealService

    @MockkBean
    private lateinit var databaseService: DatabaseService

    @MockkBean
    private lateinit var storageGridObjectAreasService: StorageGridObjectAreasService

    @MockkBean
    private lateinit var spotlessCnameService: SpotlessCnameService

    @MockkBean
    private lateinit var cnameService: CnameService

    @Autowired
    private lateinit var server: MockWebServer

    // @TestConfiguration
    // class TestConfig {
    //     @Bean
    //     fun server() = MockWebServer()
    // }

    data class QueryContent(
        var operationName: String?,
        var variables: String,
        var query: String
    )

    @Test
    fun `Database schema query used in load test`() {
        val queryContent: QueryContent = getQueryContent("src/gatling/resources/databaseSchemas_query.json")

        coEvery { affiliationService.getAllAffiliationNames() } returns listOf("paas", "demo", "notDeployed")
        coEvery { affiliationService.getAllVisibleAffiliations("test-token") } returns listOf("paas", "demo")
        coEvery { databaseService.getDatabaseSchemas(any()) } returns listOf(DatabaseSchemaResourceBuilder().build())

        webTestClient.queryGraphQL(queryContent.query, variables = mapOf("id" to "abc"), token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges") {
                graphqlData("[0].node.name").isEqualTo("paas")
                graphqlData("[0].node.databaseSchemas[0].id").isEqualTo("123")
                graphqlData("[0].node.databaseSchemas[0].application").isEqualTo("referanse")
                graphqlData("[1].node.name").isEqualTo("demo")
                graphqlData("[1].node.databaseSchemas[0].id").isEqualTo("123")
                graphqlData("[1].node.databaseSchemas[0].application").isEqualTo("referanse")
            }
    }

    @Test
    fun `Affiliation query used in load test`() {

        val queryContent: QueryContent = getQueryContent("src/gatling/resources/affiliations.json")

        coEvery { affiliationService.getAllAffiliationNames() } returns listOf("paas", "demo", "notDeployed")
        coEvery { affiliationService.getAllVisibleAffiliations("test-token") } returns listOf("paas", "demo")
        coEvery { databaseService.getDatabaseSchemas(any()) } returns listOf(DatabaseSchemaResourceBuilder().build())

        webTestClient.queryGraphQL(queryContent.query, variables = mapOf("id" to "abc"), token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges") {
                graphqlData("[0].node.name").isEqualTo("aurora")
            }
    }

    private fun getQueryContent(jsonFilename: String): QueryContent {
        val jsonFile = File(jsonFilename)
        val deploymentSpec: QueryContent = jacksonObjectMapper().readValue(jsonFile)
        return deploymentSpec
    }
}
