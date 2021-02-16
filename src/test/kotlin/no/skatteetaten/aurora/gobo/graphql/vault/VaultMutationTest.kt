package no.skatteetaten.aurora.gobo.graphql.vault

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.BooberVaultBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.BooberVault
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

    @Value("classpath:graphql/mutations/removeVaultSecrets.graphql")
    private lateinit var removeVaultSecretsMutation: Resource

    @Value("classpath:graphql/mutations/renameVaultSecret.graphql")
    private lateinit var renameVaultSecretMutation: Resource

    @Value("classpath:graphql/mutations/updateVaultSecret.graphql")
    private lateinit var updateVaultSecretMutation: Resource

    @MockkBean(relaxed = true)
    private lateinit var vaultService: VaultService

    @BeforeEach
    fun setUp() {
        coEvery { vaultService.createVault(any(), any()) } returns BooberVault(
            "test-vault",
            false,
            emptyList(),
            emptyMap()
        )
    }

    @Test
    fun `Create vault`() {
        val secrets = mapOf("name" to "latest.json", "base64Content" to "Z3VycmU=")
        val permissionList = listOf("APP_PaaS_utv")
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "test2",
                "secrets" to secrets,
                "permissions" to permissionList
            )
        )
        webTestClient.queryGraphQL(createVaultMutation, variables, "test-token")
            .expectBody()
            .graphqlData("createVault.name").isEqualTo("test-vault")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Delete vault`() {
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "test2"
            )
        )
        webTestClient.queryGraphQL(deleteVaultMutation, variables, "test-token").expectBody()
            .graphqlData("deleteVault.affiliationName").isEqualTo("aurora")
            .graphqlData("deleteVault.vaultName").isEqualTo("test2")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Add vault permission`() {
        coEvery { vaultService.addVaultPermissions(any(), any()) } returns BooberVault(
            name = "test2",
            hasAccess = true,
            permissions = listOf("APP_PaaS_utv"),
            secrets = mapOf("name" to "latest.json", "base64Content" to "Z3VycmU=")
        )
        val updatedPermissions = listOf("APP_PaaS_utv", "APP_PaaS_drift")
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "test2",
                "permissions" to updatedPermissions
            )
        )
        webTestClient.queryGraphQL(addVaultPermissionsMutation, variables, "test-token")
            .expectBody()
            .graphqlData("addVaultPermissions.name").isEqualTo("test2")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Remove vault permission`() {
        coEvery { vaultService.removeVaultPermissions(any(), any()) } returns BooberVault(
            name = "test2",
            hasAccess = true,
            permissions = listOf("APP_PaaS_utv"),
            secrets = mapOf("name" to "latest.json", "base64Content" to "Z3VycmU=")
        )
        val removePermission = listOf("APP_PaaS_drift")
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "test2",
                "permissions" to removePermission
            )
        )
        webTestClient.queryGraphQL(removeVaultPermissionsMutation, variables, "test-token")
            .expectBody()
            .graphqlData("removeVaultPermissions.name").isEqualTo("test2")
            .graphqlData("removeVaultPermissions.permissions[0]").isEqualTo("APP_PaaS_utv")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Add vault secrets`() {
        coEvery { vaultService.addVaultSecrets(any(), any()) } returns BooberVaultBuilder(
            secrets = mapOf(
                "secret1" to "dGVzdA==",
                "name" to "dGVzdA=="
            )
        ).build()

        val input = AddVaultSecretsInput("aurora", "test-vault", listOf(Secret("name", "dGVzdA==")))
        webTestClient.queryGraphQL(addVaultSecretsMutation, input, "test-token")
            .expectBody()
            .graphqlData("addVaultSecrets.name").isEqualTo("test-vault")
            .graphqlData("addVaultSecrets.secrets.length()").isEqualTo(2)
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Remove vault secrets`() {
        coEvery { vaultService.removeVaultSecrets(any(), any()) } returns BooberVaultBuilder().build()

        val input = RemoveVaultSecretsInput("aurora", "test-vault", listOf(Secret("name", "dGVzdA==")))
        webTestClient.queryGraphQL(removeVaultSecretsMutation, input, "test-token")
            .expectBody()
            .graphqlData("removeVaultSecrets.name").isEqualTo("test-vault")
            .graphqlData("removeVaultSecrets.secrets").isNotEmpty
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Rename vault secret`() {
        coEvery { vaultService.renameVaultSecret(any(), any(), any()) } returns BooberVaultBuilder().build()

        val input = RenameVaultSecretInput("aurora", "test-vault", "latest.properties", "newest.properties")
        webTestClient.queryGraphQL(renameVaultSecretMutation, input, "test-token")
            .expectBody()
            .graphqlData("renameVaultSecret.name").isEqualTo("test-vault")
            .graphqlData("renameVaultSecret.secrets").isNotEmpty
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Update vault secret`() {
        coEvery { vaultService.updateVaultSecret(any(), any(), any()) } returns BooberVaultBuilder().build()

        val input = UpdateVaultSecretInput(
            affiliationName = "aurora",
            vaultName = "test-vaukt",
            secretName = "latest.properties",
            base64Content = "dGVzdDEyMw=="
        )
        webTestClient.queryGraphQL(updateVaultSecretMutation, input, "test-token")
            .expectBody()
            .graphqlData("updateVaultSecret.name").isEqualTo("test-vault")
            .graphqlDoesNotContainErrors()
    }
}
