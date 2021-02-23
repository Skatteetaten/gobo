package no.skatteetaten.aurora.gobo.integration.boober

import assertk.Assert
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import no.skatteetaten.aurora.gobo.BooberVaultBuilder
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.graphql.vault.RenameVaultInput
import no.skatteetaten.aurora.gobo.graphql.vault.Secret
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.bodyAsObject
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

class VaultServiceTest {
    private val affiliationName = "aurora"
    private val vaultName = "test-vault"
    private val renamedVaultName = "renamed-test-vault"

    private val server = MockWebServer()
    private val url = server.url("/")

    private val vaultService = VaultService(BooberWebClient(url.toString(), WebClient.create(), testObjectMapper()))

    @Test
    fun `Get vault`() {
        val response = Response(BooberVaultBuilder().build())
        val requests = server.executeBlocking(response) {
            val vault = vaultService.getVault(
                VaultContext(
                    token = "token",
                    affiliationName = affiliationName,
                    vaultName = vaultName
                )
            )
            assertThat(vault.name).isEqualTo(vaultName)
            assertThat(vault.hasAccess).isTrue()
            assertThat(vault.permissions?.get(0)).isEqualTo("APP_PaaS_utv")
            assertThat(vault.secrets?.get("latest.properties")).isEqualTo("QVRTX1VTRVJOQU1FPWJtYwp")
        }
        assertThat(requests).hasSize(1)
        assertThat(requests).containsGetVaultRequest()
    }

    @Test
    fun `Get vaults`() {
        val response = Response(BooberVaultBuilder().build())
        val requests = server.executeBlocking(response) {
            val vault = vaultService.getVaults(token = "token", affiliationName = affiliationName)
            assertThat(vault[0].name).isEqualTo(vaultName)
            assertThat(vault[0].hasAccess).isTrue()
            assertThat(vault[0].permissions?.get(0)).isEqualTo("APP_PaaS_utv")
            assertThat(vault[0].secrets?.get("latest.properties")).isEqualTo("QVRTX1VTRVJOQU1FPWJtYwp")
        }
        assertThat(requests).hasSize(1)
        assertThat(requests.first()?.path).isEqualTo("/v1/vault/$affiliationName")
    }

    @Test
    fun `Rename vault sunshine`() {
        val getVaultResponse = BooberVaultBuilder().build()
        val putVaultResponse = getVaultResponse.copy(name = renamedVaultName)
        val deleteVaultResponse = getVaultResponse.copy()

        val requests = server.executeBlocking(
            200 to Response(items = listOf(getVaultResponse)),
            404 to Response(items = listOf(BooberVaultBuilder().build())),
            200 to Response(items = listOf(putVaultResponse)),
            200 to Response(items = listOf(getVaultResponse)),
            200 to Response(items = listOf(deleteVaultResponse))
        ) {
            val renamedVault = vaultService.renameVault(
                token = "token",
                RenameVaultInput(
                    affiliationName = affiliationName,
                    vaultName = vaultName,
                    newVaultName = renamedVaultName
                )
            )
            assertThat(renamedVault.name).isEqualTo("renamed-test-vault")
            assertThat(renamedVault.hasAccess).isTrue()
            assertThat(renamedVault.permissions).isEqualTo(
                listOf("APP_PaaS_utv")
            )
            assertThat(renamedVault.secrets).isEqualTo(
                mapOf("latest.properties" to "QVRTX1VTRVJOQU1FPWJtYwp")
            )
        }
    }

    @Test
    fun `Rename vault failed because new name already exists`() {
        val getVaultResponse = BooberVaultBuilder().build()
        val checkIfVaultExistsResponse = BooberVaultBuilder().build()
        val putVaultResponse = getVaultResponse.copy(name = renamedVaultName)
        val deleteVaultResponse = getVaultResponse.copy()

        val requests = server.executeBlocking(
            Response(getVaultResponse),
            Response(checkIfVaultExistsResponse),
            putVaultResponse,
            deleteVaultResponse
        ) {
            try {
                vaultService.renameVault(
                    token = "token",
                    RenameVaultInput(
                        affiliationName = affiliationName,
                        vaultName = vaultName,
                        newVaultName = renamedVaultName
                    )
                )
            } catch (e: GoboException) {
                assertThat(e.errorMessage).isEqualTo("Vault with vault name renamed-test-vault already exists.")
            }
        }
    }

    @Test
    fun `Delete vaults`() {
        val getVaultResponse = BooberVaultBuilder(permissions = listOf("permission1")).build()
        val response = Response<String>(items = emptyList())
        val requests = server.executeBlocking(Response(getVaultResponse), response) {
            vaultService.deleteVault(
                VaultContext(
                    token = "token",
                    affiliationName = affiliationName,
                    vaultName = vaultName
                )
            )
        }
        assertThat(requests).containsGetVaultRequest()
        assertThat(requests).hasSize(2)
    }

    @Test
    fun `Add permission to vault`() {
        val getVaultResponse = BooberVaultBuilder(permissions = listOf("permission1")).build()
        val putVaultResponse = getVaultResponse.copy(permissions = listOf("permission1", "permission2"))

        val requests = server.executeBlocking(
            Response(getVaultResponse),
            Response(putVaultResponse)
        ) {
            val booberVault = vaultService.addVaultPermissions(
                VaultContext(
                    token = "token",
                    affiliationName = affiliationName,
                    vaultName = vaultName
                ),
                permissions = listOf("permission2")
            )
            assertThat(booberVault).isNotNull()
        }
        assertThat(requests).containsGetVaultRequest()
        assertThat(requests.last()?.bodyAsObject<BooberVaultInput>()?.permissions).isEqualTo(
            listOf(
                "permission1",
                "permission2"
            )
        )
    }

    @Test
    fun `Remove permission from vault`() {
        val getVaultResponse = BooberVaultBuilder(permissions = listOf("permission1", "permission2")).build()
        val putVaultResponse = getVaultResponse.copy(permissions = listOf("permission1"))

        val requests = server.executeBlocking(
            Response(getVaultResponse),
            Response(putVaultResponse)
        ) {
            val booberVault = vaultService.removeVaultPermissions(
                VaultContext(
                    token = "token",
                    affiliationName = affiliationName,
                    vaultName = vaultName,
                ),
                permissions = listOf("permission1")
            )
            assertThat(booberVault).isNotNull()
        }
        assertThat(requests).containsGetVaultRequest()
        assertThat(requests.last()?.bodyAsObject<BooberVaultInput>()?.permissions).isEqualTo(listOf("permission2"))
    }

    @Test
    fun `Add secrets to vault`() {
        val getVaultResponse = BooberVaultBuilder(secrets = mapOf("secret1" to "YWJj")).build()
        val putVaultResponse = getVaultResponse.copy(secrets = mapOf("secret2" to "MTIz"))

        val requests = server.executeBlocking(Response(getVaultResponse), Response(putVaultResponse)) {
            val vault = vaultService.addVaultSecrets(
                VaultContext(
                    token = "token",
                    affiliationName = affiliationName,
                    vaultName = vaultName
                ),
                secrets = listOf(Secret("secret2", "MTIz"))
            )
            assertThat(vault).isNotNull()
        }
        assertThat(requests).containsGetVaultRequest()
        assertThat(
            requests.last()?.bodyAsObject<BooberVaultInput>()?.secrets
        ).isEqualTo(mapOf("secret1" to "YWJj", "secret2" to "MTIz"))
    }

    @Test
    fun `Rename vault secret`() {
        val getVaultResponse = BooberVaultBuilder(secrets = mapOf("secret1" to "YWJj", "a" to "b")).build()
        val putVaultResponse = getVaultResponse.copy(secrets = mapOf("secret2" to "YWJj", "a" to "b"))

        val requests = server.executeBlocking(Response(getVaultResponse), Response(putVaultResponse)) {
            val vault = vaultService.renameVaultSecret(
                ctx = VaultContext(
                    token = "token",
                    affiliationName = affiliationName,
                    vaultName = vaultName
                ),
                secretName = "secret1",
                newSecretName = "secret2"
            )
            assertThat(vault).isNotNull()
        }

        assertThat(requests).containsGetVaultRequest()
        assertThat(requests.last()?.bodyAsObject<BooberVaultInput>()?.secrets).isEqualTo(mapOf("secret2" to "YWJj", "a" to "b"))
    }

    private fun Assert<List<RecordedRequest?>>.containsGetVaultRequest() = given {
        if (it.first()?.path == "/v1/vault/$affiliationName/$vaultName") return
        expected("First request to get vault but was ${it.first()}")
    }
}
