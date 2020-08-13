package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.WebsealService
import no.skatteetaten.aurora.gobo.integration.skap.WebsealStateResource
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.printResult
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import reactor.core.publisher.Mono

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
        every { affiliationService.getAllAffiliations() } returns Mono.just(
            listOf(
                "paas",
                "demo"
            )
        ) // FIXME mono warning

        webTestClient.queryGraphQL(getAffiliationsQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.items") {
                graphqlData("[0].name").isEqualTo("paas")
                graphqlData("[1].name").isEqualTo("demo")
            }
            .graphqlData("affiliations.totalCount").isEqualTo(2)
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for visible affiliations`() {
        every { affiliationService.getAllVisibleAffiliations("test-token") } returns Mono.just(
            listOf(
                "paas",
                "demo"
            )
        ) // FIXME mono warning

        webTestClient.queryGraphQL(
            getAffiliationsWithVisibilityQuery,
            mapOf("checkForVisibility" to true),
            "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.items") {
                graphqlData("[0].name").isEqualTo("paas")
                graphqlData("[1].name").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliation`() {
        webTestClient.queryGraphQL(getAffiliationQuery, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.items[0].name").isEqualTo("aurora")
            .graphqlData("affiliations.totalCount").isEqualTo(1)
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliations with database schemas`() {
        every { affiliationService.getAllAffiliations() } returns Mono.just(listOf("paas")) // FIXME mono warning
        every { databaseService.getDatabaseSchemas(any()) } returns Mono.just(listOf(DatabaseSchemaResourceBuilder().build()))

        webTestClient.queryGraphQL(getAffiliationsWithDatabaseSchemaQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.totalCount").isEqualTo(1)
            .graphqlDataWithPrefix("affiliations.items[0]") {
                graphqlData("name").isEqualTo("paas")
                graphqlData("databaseSchemas[0].id").isEqualTo("123")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliations with webseal states`() {
        every { affiliationService.getAllAffiliations() } returns Mono.just(listOf("paas")) // FIXME mono warning
        every { applicationService.getApplications(any()) } returns listOf(ApplicationResourceBuilder().build())
        every { websealService.getStates() } returns listOf(WebsealStateResourceBuilder().build())

        webTestClient.queryGraphQL(getAffiliationsWithWebsealStatesQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .printResult()
            /*
            .graphqlData("affiliations.totalCount").isEqualTo("1")
            .graphqlDataWithPrefix("affiliations.items[0]") {
                graphqlData("name").isEqualTo("paas")
                graphqlData("websealStates[0].name").isEqualTo("test.no")
            }
            .graphqlDoesNotContainErrors()*/
    }
}
