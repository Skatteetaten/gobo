package no.skatteetaten.aurora.gobo.graphql.vault

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
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
import no.skatteetaten.aurora.gobo.graphql.printResult

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

    @MockkBean(relaxed = true)
    private lateinit var vaultService: VaultService

    @BeforeEach
    fun setUp() {
        coEvery { vaultService.createVault(any(), any()) } returns BooberVault("test-vault", false, emptyList(), emptyMap())
    }

    @Test
    fun `Create vault`() {
        val secrets = mapOf("name" to "latest.json", "base64Content" to "Z3VycmU=")
        val permissionList = listOf("APP_PaaS_utv")
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "gurre-test2",
                "secrets" to secrets,
                "permissions" to permissionList
            )
        )
        webTestClient.queryGraphQL(createVaultMutation, variables, "test-token")
            .expectBody()
            .printResult()
        // .graphqlData("createVault.name").isEqualTo("test-vault")
        // .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Delete vault`() {
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "gurre-test2"
            )
        )
        webTestClient.queryGraphQL(deleteVaultMutation, variables, "test-token").expectBody()
            .graphqlData("deleteVault.affiliationName").isEqualTo("aurora")
            .graphqlData("deleteVault.vaultName").isEqualTo("gurre-test2")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Add vault permission`() {
        coEvery { vaultService.addVaultPermissions(any(), any(), any(), any()) } returns BooberVault(
            name = "gurre-test2",
            hasAccess = true,
            permissions = listOf("APP_PaaS_utv"),
            secrets = mapOf("name" to "latest.json", "base64Content" to "Z3VycmU=")
        )
        val updatedPermissions = listOf("APP_PaaS_utv", "APP_PaaS_drift")
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "gurre-test2",
                "permissions" to updatedPermissions
            )
        )
        webTestClient.queryGraphQL(addVaultPermissionsMutation, variables, "test-token")
            .expectBody()
            .graphqlData("addVaultPermissions.name").isEqualTo("gurre-test2")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Remove vault permission`() {
        coEvery { vaultService.removeVaultPermissions(any(), any(), any(), any()) } returns BooberVault(
            name = "gurre-test2",
            hasAccess = true,
            permissions = listOf("APP_PaaS_utv"),
            secrets = mapOf("name" to "latest.json", "base64Content" to "Z3VycmU=")
        )
        val removePermission = listOf("APP_PaaS_drift")
        val variables = mapOf(
            "input" to mapOf(
                "affiliationName" to "aurora",
                "vaultName" to "gurre-test2",
                "permissions" to removePermission
            )
        )
        webTestClient.queryGraphQL(removeVaultPermissionsMutation, variables, "test-token")
            .expectBody()
            .graphqlData("removeVaultPermissions.name").isEqualTo("gurre-test2")
            .graphqlData("removeVaultPermissions.permissions[0]").isEqualTo("APP_PaaS_utv")
            .graphqlDoesNotContainErrors()
    }
}
