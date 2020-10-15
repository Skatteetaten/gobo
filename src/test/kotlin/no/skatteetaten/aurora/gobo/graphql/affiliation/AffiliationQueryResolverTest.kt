package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.WebsealService
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled
class AffiliationQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getAffiliations.graphql")
    private lateinit var getAffiliationsQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithVisibilityCheck.graphql")
    private lateinit var getAffiliationsWithVisibilityQuery: Resource

    @Value("classpath:graphql/queries/getAffiliation.graphql")
    private lateinit var getAffiliationQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithDatabaseSchema.graphql")
    private lateinit var getAffiliationsWithDatabaseSchemaQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithWebsealStates.graphql")
    private lateinit var getAffiliationsWithWebsealStatesQuery: Resource

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @MockkBean
    private lateinit var databaseService: DatabaseServiceReactive

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockkBean
    private lateinit var websealService: WebsealService

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Query for all affiliations`() {
        coEvery { affiliationService.getAllAffiliations() } returns listOf("paas", "demo")

        webTestClient.queryGraphQL(getAffiliationsQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges") {
                graphqlData("[0].node.name").isEqualTo("paas")
                graphqlData("[1].node.name").isEqualTo("demo")
            }
            .graphqlData("affiliations.totalCount").isEqualTo(2)
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for visible affiliations`() {
        coEvery { affiliationService.getAllVisibleAffiliations("test-token") } returns listOf("paas", "demo")

        webTestClient.queryGraphQL(
            getAffiliationsWithVisibilityQuery,
            mapOf("checkForVisibility" to true),
            "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges") {
                graphqlData("[0].node.name").isEqualTo("paas")
                graphqlData("[1].node.name").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliation`() {
        webTestClient.queryGraphQL(getAffiliationQuery, mapOf("name" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.edges[0].node.name").isEqualTo("aurora")
            .graphqlData("affiliations.totalCount").isEqualTo(1)
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliations with database schemas`() {
        coEvery { affiliationService.getAllAffiliations() } returns listOf("paas")
        coEvery { databaseService.getDatabaseSchemas(any()) } returns listOf(DatabaseSchemaResourceBuilder().build())

        webTestClient.queryGraphQL(getAffiliationsWithDatabaseSchemaQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.totalCount").isEqualTo(1)
            .graphqlDataWithPrefix("affiliations.edges[0].node") {
                graphqlData("name").isEqualTo("paas")
                graphqlData("databaseSchemas[0].id").isEqualTo("123")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliations with webseal states`() {
        coEvery { affiliationService.getAllAffiliations() } returns listOf("paas")
        every { applicationService.getApplications(any()) } returns listOf(ApplicationResourceBuilder().build())
        coEvery { websealService.getStates() } returns listOf(WebsealStateResourceBuilder().build())

        webTestClient.queryGraphQL(getAffiliationsWithWebsealStatesQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.totalCount").isEqualTo("1")
            .graphqlDataWithPrefix("affiliations.edges[0].node") {
                graphqlData("name").isEqualTo("paas")
                graphqlData("websealStates[0].name").isEqualTo("test.no")
            }
            .graphqlDoesNotContainErrors()
    }
}
