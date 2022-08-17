package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.CnameAzureBuilder
import no.skatteetaten.aurora.gobo.CnameInfoBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchemaDataLoader
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealStateDataLoader
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.WebsealService
import no.skatteetaten.aurora.gobo.service.AffiliationService
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import no.skatteetaten.aurora.gobo.StoragegridObjectAreaResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.cname.CnameAzureDataLoader
import no.skatteetaten.aurora.gobo.graphql.cname.CnameInfoDataLoader
import no.skatteetaten.aurora.gobo.graphql.storagegrid.StorageGridObjectAreaDataLoader
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import no.skatteetaten.aurora.gobo.integration.mokey.StorageGridObjectAreasService
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService

@Import(
    AffiliationQuery::class,
    WebsealAffiliationService::class,
    WebsealStateDataLoader::class,
    DatabaseSchemaDataLoader::class,
    StorageGridObjectAreaDataLoader::class,
    CnameAzureDataLoader::class,
    CnameInfoDataLoader::class
)
class AffiliationQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getAffiliations.graphql")
    private lateinit var getAffiliationsQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsIncludeUndeployed.graphql")
    private lateinit var getAffiliationsIncludeUndeployedQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithVisibilityCheck.graphql")
    private lateinit var getAffiliationsWithVisibilityQuery: Resource

    @Value("classpath:graphql/queries/getAffiliation.graphql")
    private lateinit var getAffiliationQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithDatabaseSchema.graphql")
    private lateinit var getAffiliationsWithDatabaseSchemaQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithStorageGridObjectAreas.graphql")
    private lateinit var getAffiliationsWithStorageGridObjectAreasQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithWebsealStates.graphql")
    private lateinit var getAffiliationsWithWebsealStatesQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithCname.graphql")
    private lateinit var getAffiliationsWithCnameQuery: Resource

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @MockkBean
    private lateinit var databaseService: DatabaseService

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var websealService: WebsealService

    @MockkBean
    private lateinit var storageGridObjectAreasService: StorageGridObjectAreasService

    @MockkBean
    private lateinit var cnameService: CnameService

    @MockkBean
    private lateinit var spotlessCnameService: SpotlessCnameService

    @Test
    fun `Query fo all affiliations include undeployed`() {
        coEvery { affiliationService.getAllDeployedAffiliations() } returns listOf("paas", "demo")
        coEvery { affiliationService.getAllAffiliationNames() } returns listOf("paas", "demo", "notDeployed")

        webTestClient.queryGraphQL(getAffiliationsIncludeUndeployedQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges") {
                graphqlData("[0].node.name").isEqualTo("paas")
                graphqlData("[1].node.name").isEqualTo("demo")
                graphqlData("[2].node.name").isEqualTo("notDeployed")
            }
            .graphqlData("affiliations.totalCount").isEqualTo(3)
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for all deployed affiliations`() {
        coEvery { affiliationService.getAllDeployedAffiliations() } returns listOf("paas", "demo")

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
        coEvery { affiliationService.getAllDeployedAffiliations() } returns listOf("paas")
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
        coEvery { affiliationService.getAllDeployedAffiliations() } returns listOf("paas")
        coEvery { applicationService.getApplications(any()) } returns listOf(ApplicationResourceBuilder().build())
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

    @Test
    fun `Query for affiliations with storageGrid objectAreas`() {
        coEvery { affiliationService.getAllDeployedAffiliations() } returns listOf("aup")
        coEvery { storageGridObjectAreasService.getObjectAreas(any(), any()) } returns listOf(
            StoragegridObjectAreaResourceBuilder("aup").build()
        )

        webTestClient.queryGraphQL(getAffiliationsWithStorageGridObjectAreasQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.totalCount").isEqualTo("1")
            .graphqlDataWithPrefix("affiliations.edges[0].node") {
                graphqlData("name").isEqualTo("aup")
                graphqlData("storageGrid.objectAreas.active[0].name").isEqualTo("some-area")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliations with cname configured`() {
        coEvery { affiliationService.getAllDeployedAffiliations() } returns listOf("aup")
        coEvery { cnameService.getCnameInfo(any()) }.returns(listOf(CnameInfoBuilder().build()))
        coEvery { spotlessCnameService.getCnameContent(any()) }.returns(listOf(CnameAzureBuilder().build()))

        webTestClient.queryGraphQL(getAffiliationsWithCnameQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.totalCount").isEqualTo("1")
            .graphqlDataWithPrefix("affiliations.edges[0].node") {
                graphqlData("name").isEqualTo("aup")
                graphqlData("cname.onPrem[0].appName").isEqualTo("demo")
                graphqlData("cname.azure[0].ownerObjectName").isEqualTo("demo-azure")
                graphqlData("cname.onPrem[1]").doesNotExist()
            }
    }
}
