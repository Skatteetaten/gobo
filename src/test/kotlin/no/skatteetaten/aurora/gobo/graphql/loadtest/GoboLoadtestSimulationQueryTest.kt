package no.skatteetaten.aurora.gobo.graphql.loadtest

import java.io.File
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchemaDataLoader
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.service.AffiliationService

@Import(
    AffiliationQuery::class,
    DatabaseSchemaDataLoader::class,
)
class GoboLoadtestSimulationQueryTest : GraphQLTestWithDbhAndSkap() {

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @MockkBean
    private lateinit var databaseService: DatabaseService

    data class QueryContent(
        var operationName: String?,
        var variables: String,
        var query: String
    )

    @Test
    fun `Database schema query used in load test`() {
        val queryContent: QueryContent = "src/gatling/resources/databaseSchemas_query.json".jsonQueryContent
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
        val queryContent: QueryContent = "src/gatling/resources/affiliations.json".jsonQueryContent
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

    private val String.jsonQueryContent get() =
        jacksonObjectMapper().convertValue<QueryContent>(jacksonObjectMapper().readTree(File(this)))
}
