package no.skatteetaten.aurora.gobo.graphql.vault

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService

@Import(AffiliationQuery::class, VaultMutation::class) //Note: Framework need to import a query to run a mutation, the reason for this import...
class VaultMutationTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/createVault.graphql")
    private lateinit var createVaultMutation: Resource

    @MockkBean(relaxed = true)
    private lateinit var vaultService: VaultService

    @MockkBean(relaxed = true)
    private lateinit var affiliationService: AffiliationService //Note: neccessary be cause of the irrelevant import of AffiliationQuery::class....

    @BeforeEach
    fun setUp() {
        coEvery { vaultService.createVault(any(), any()) } returns Vault("test-vault", false, emptyList(), emptyMap())
    }

    @Test
    fun `Create vault`() {
        val fileMap = mapOf("name" to "latest.json", "base64Content" to "Z3VycmU=")
        val permissionList = listOf("APP_PaaS_utv")
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "gurre-test2",
                "files" to fileMap,
                "permissions" to permissionList
            )
        )
        webTestClient.queryGraphQL(createVaultMutation, variables, "test-token").expectBody()
            .graphqlData("createVault.name").isEqualTo("test-vault")
            .graphqlDoesNotContainErrors()
    }
}
