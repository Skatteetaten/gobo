package no.skatteetaten.aurora.gobo.graphql.vault

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.BooberVaultBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(VaultMutation::class)
class VaultMutationTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/createVault.graphql")
    private lateinit var createVaultMutation: Resource

    @Value("classpath:graphql/mutations/deleteVault.graphql")
    private lateinit var deleteVaultMutation: Resource

    @Value("classpath:graphql/mutations/addVaultPermissions.graphql")
    private lateinit var addVaultPermissionsMutation: Resource

    @Value("classpath:graphql/mutations/removeVaultPermissions.graphql")
    private lateinit var removeVaultPermissionsMutation: Resource

    @Value("classpath:graphql/mutations/addVaultSecrets.graphql")
    private lateinit var addVaultSecretsMutation: Resource

    @MockkBean(relaxed = true)
    private lateinit var vaultService: VaultService

    @BeforeEach
    fun setUp() {
        coEvery { vaultService.createVault(any(), any()) } returns BooberVaultBuilder().build()
    }

    @Test
    fun `Create vault`() {
        val input = CreateVaultInput(
            affiliationName = "aurora",
            vaultName = "test-vault",
            secrets = listOf(Secret("latest.json", "Z3VycmU=")),
            permissions = listOf("APP_PaaS_utv")
        )
        webTestClient.queryGraphQL(createVaultMutation, input, "test-token")
            .expectBody()
            .graphqlData("createVault.name").isEqualTo("test-vault")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Delete vault`() {
        val input = DeleteVaultInput("aurora", "test-vault")
        webTestClient.queryGraphQL(deleteVaultMutation, input, "test-token").expectBody()
            .graphqlData("deleteVault.affiliationName").isEqualTo("aurora")
            .graphqlData("deleteVault.vaultName").isEqualTo("test-vault")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Add vault permission`() {
        coEvery { vaultService.addVaultPermissions(any(), any(), any(), any()) } returns BooberVaultBuilder().build()
        val input = AddVaultPermissionsInput("aurora", "test-vault", listOf("APP_PaaS_utv", "APP_PaaS_drift"))
        webTestClient.queryGraphQL(addVaultPermissionsMutation, input, "test-token")
            .expectBody()
            .graphqlData("addVaultPermissions.name").isEqualTo("test-vault")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Remove vault permission`() {
        coEvery { vaultService.removeVaultPermissions(any(), any(), any(), any()) } returns BooberVaultBuilder().build()
        val input = RemoveVaultPermissionsInput("aurora", "test-vault", listOf("APP_PaaS_drift"))
        webTestClient.queryGraphQL(removeVaultPermissionsMutation, input, "test-token")
            .expectBody()
            .graphqlData("removeVaultPermissions.name").isEqualTo("test-vault")
            .graphqlData("removeVaultPermissions.permissions[0]").isEqualTo("APP_PaaS_utv")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Add vault secrets`() {
        coEvery { vaultService.addVaultSecrets(any(), any(), any(), any()) } returns BooberVaultBuilder().build()

        val input = AddVaultSecretsInput(
            affiliationName = "aurora",
            vaultName = "test-vault",
            secrets = listOf(Secret("name", "Z3VycmU="))
        )
        webTestClient.queryGraphQL(addVaultSecretsMutation, input, "test-token")
            .expectBody()
            .graphqlData("addVaultSecrets.name").isEqualTo("test-vault")
            .graphqlData("addVaultSecrets.secrets.length()").isEqualTo(2)
    }
}
