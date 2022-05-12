package no.skatteetaten.aurora.gobo.graphql.storagegrid

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.HerkimerResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirstContainsMessage
import no.skatteetaten.aurora.gobo.graphql.isFalse
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerIntegrationException
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.ResourceKind
import no.skatteetaten.aurora.gobo.service.AffiliationService

@Import(AffiliationQuery::class, StorageGridTenantDataLoader::class)
class StorageGridTenantTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/queries/getStorageGridTenantIsRegistered.graphql")
    private lateinit var getTenantIsRegistered: Resource

    @MockkBean
    private lateinit var herkimerService: HerkimerService
    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @Test
    fun `Get tenant is registered`() {
        coEvery { herkimerService.getResourceWithClaim(any(), ResourceKind.StorageGridTenant) } returns HerkimerResourceBuilder("1").build()

        webTestClient.queryGraphQL(
            queryResource = getTenantIsRegistered,
            variables = mapOf("affiliations" to listOf("aup", "aup")),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDoesNotContainErrors()
            .graphqlData("affiliations.edges[0].node.storageGrid.tenant.isRegistered").isTrue()
    }

    @Test
    fun `Should return not registered when no tenant in herkimer`() {
        coEvery { herkimerService.getResourceWithClaim(any(), ResourceKind.StorageGridTenant) } returns null

        webTestClient.queryGraphQL(
            queryResource = getTenantIsRegistered,
            variables = mapOf("affiliations" to listOf("aup", "aup")),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDoesNotContainErrors()
            .graphqlData("affiliations.edges[0].node.storageGrid.tenant.isRegistered").isFalse()
    }
    @Test
    fun `Should throw when multiple tenants`() {
        coEvery { herkimerService.getResourceWithClaim(any(), ResourceKind.StorageGridTenant) } throws HerkimerIntegrationException("Expected 1 resource but got 2")

        webTestClient.queryGraphQL(
            queryResource = getTenantIsRegistered,
            variables = mapOf("affiliations" to listOf("aup", "aup")),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirstContainsMessage("Expected 1 resource but got 2")
    }
}
