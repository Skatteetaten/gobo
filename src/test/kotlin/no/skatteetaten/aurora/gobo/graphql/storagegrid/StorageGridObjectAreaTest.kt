package no.skatteetaten.aurora.gobo.graphql.storagegrid

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.StoragegridObjectAreaResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.mokey.StorageGridObjectAreasService
import no.skatteetaten.aurora.gobo.service.AffiliationService

@Import(AffiliationQuery::class, StorageGridObjectAreaDataLoader::class)
class StorageGridObjectAreaTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/queries/getStorageGridObjectAreas.graphql")
    private lateinit var getStoragegridObjectAreas: Resource

    @MockkBean
    private lateinit var storageGridObjectAreasService: StorageGridObjectAreasService

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @Test
    fun `Get storeageGridObjectAreas`() {
        coEvery { storageGridObjectAreasService.getObjectAreas(any(), any()) } returns listOf(
            StoragegridObjectAreaResourceBuilder("aup").build(),
            StoragegridObjectAreaResourceBuilder("aup").build()
        )

        webTestClient.queryGraphQL(
            queryResource = getStoragegridObjectAreas,
            variables = mapOf("affiliations" to listOf("aup", "aup")),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDoesNotContainErrors()
            .graphqlDataWithPrefix("affiliations.edges[0].node.storageGrid.objectAreas.active") {
                graphqlData("length()").isEqualTo(2)
                graphqlDataFirst("name").isEqualTo("some-area")
                graphqlDataFirst("bucketName").isEqualTo("aup-utv04-default")
            }
    }
    @Test
    fun `Get StorageGridObjectAreas when no areas`() {
        coEvery { storageGridObjectAreasService.getObjectAreas(any(), any()) } returns emptyList()

        webTestClient.queryGraphQL(
            queryResource = getStoragegridObjectAreas,
            variables = mapOf("affiliations" to listOf("aup", "aup")),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDoesNotContainErrors()
            .graphqlData("affiliations.edges[0].node.storageGrid.objectAreas.active.length()").isEqualTo(0)
    }
}
