package no.skatteetaten.aurora.gobo.graphql.vault

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService

@Import(
    AffiliationQuery::class,
    VaultDataLoader::class
)
class VaultQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getVaults.graphql")
    private lateinit var getVaultsQuery: Resource

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @MockkBean
    private lateinit var vaultService: VaultService

    private val vault = Vault(
        name = "boober",
        hasAccess = true,
        permissions = emptyList(),
        secrets = emptyMap()
    )

    @Test
    fun `Query for vault`() {
        // coEvery { affiliationService.getAllAffiliations() } returns listOf("paas", "demo")
        coEvery { vaultService.getVault(any(), any(), any()) } returns vault

        val variables = mapOf("affiliationNames" to listOf("aurora"), "vaultNames" to listOf("boober"))
        webTestClient.queryGraphQL(getVaultsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .printResult()
        // .graphqlData("affiliations.edges[0].node.name").isEqualTo("aurora")
        // .graphqlData("affiliations.totalCount").isEqualTo(1)
        // .graphqlDoesNotContainErrors()
    }

    // @Test
    // fun `Query for all vaults`() {
    //     coEvery { affiliationService.getAllAffiliations() } returns listOf("paas")
    //
    //     webTestClient.queryGraphQL(getAffiliationsQuery, token = "test-token")
    //         .expectStatus().isOk
    //         .expectBody()
    //         .graphqlDataWithPrefix("affiliations.edges") {
    //             graphqlData("[0].node.name").isEqualTo("paas")
    //             graphqlData("[1].node.name").isEqualTo("demo")
    //         }
    //         .graphqlData("affiliations.totalCount").isEqualTo(2)
    //         .graphqlDoesNotContainErrors()
    // }
}
