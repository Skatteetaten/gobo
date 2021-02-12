package no.skatteetaten.aurora.gobo.integration.boober

import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.graphql.vault.DeleteVaultInput
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.bodyAsObject
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer

internal class VaultServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")
    private val input = DeleteVaultInput("aurora", "boober")

    private val vaultService =
        VaultService(BooberWebClient(url.toString(), WebClient.create(), testObjectMapper()))

    private val vaultSimple = BooberVault(
        name = "boober",
        hasAccess = true,
        permissions = listOf("APP_PaaS_utv"),
        secrets = mapOf("latest.properties" to "QVRTX1VTRVJOQU1FPWJtYwp")
    )

    @Test
    fun `Get vault`() {
        val response = Response(items = listOf(vaultSimple))
        val requests = server.executeBlocking(response) {
            val vault = vaultService.getVault(affiliationName = "aurora", vaultName = "boober", token = "token")
            assertThat(vault.name).isEqualTo("boober")
            assertThat(vault.hasAccess).isTrue()
            assertThat(vault.permissions?.get(0)).isEqualTo("APP_PaaS_utv")
            assertThat(vault.secrets?.get("latest.properties")).isEqualTo("QVRTX1VTRVJOQU1FPWJtYwp")
        }
        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get vaults`() {
        val response = Response(items = listOf(vaultSimple))
        val requests = server.executeBlocking(response) {
            val vault = vaultService.getVaults(affiliationName = "aurora", token = "token")
            assertThat(vault[0].name).isEqualTo("boober")
            assertThat(vault[0].hasAccess).isTrue()
            assertThat(vault[0].permissions?.get(0)).isEqualTo("APP_PaaS_utv")
            assertThat(vault[0].secrets?.get("latest.properties")).isEqualTo("QVRTX1VTRVJOQU1FPWJtYwp")
        }
        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Delete vaults`() {
        val affiliationName = "aurora"
        val vaultName = "boober"
        val response = Response<String>(items = emptyList())
        val requests = server.executeBlocking(response) {
            vaultService.deleteVault(affiliationName = affiliationName, token = "token", vaultName = vaultName)
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/vault/$affiliationName/$vaultName")
        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Add permission to vault`() {

        val affiliationName = "aurora"
        val vaultName = "boober"
        val getVaultResponse = BooberVault(
            name = vaultName,
            hasAccess = true,
            permissions = listOf("permission1"),
            secrets = mapOf("latest.properties" to "QVRTX1VTRVJOQU1FPWJtYwp")
        )
        val putVaultResponse = getVaultResponse.copy(permissions = listOf("permission1", "permission2"))

        val requests = server.executeBlocking(
            Response(getVaultResponse),
            Response(putVaultResponse)
        ) {
            val booberVault = vaultService.addVaultPermissions(
                token = "token",
                affiliationName = affiliationName,
                vaultName = vaultName,
                permissions = listOf("permission2")
            )
            assertThat(booberVault).isNotNull()
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/vault/$affiliationName/$vaultName")
        assertThat(requests.last()?.bodyAsObject<BooberVaultInput>()?.permissions).isEqualTo(listOf("permission1", "permission2"))
    }

    @Test
    fun `Remove permission from vault`() {

        val affiliationName = "aurora"
        val vaultName = "boober"
        val getVaultResponse = BooberVault(
            name = vaultName,
            hasAccess = true,
            permissions = listOf("permission1", "permission2"),
            secrets = mapOf("latest.properties" to "QVRTX1VTRVJOQU1FPWJtYwp")
        )
        val putVaultResponse = getVaultResponse.copy(permissions = listOf("permission1"))

        val requests = server.executeBlocking(
            Response(getVaultResponse),
            Response(putVaultResponse)
        ) {
            val booberVault = vaultService.removeVaultPermissions(
                token = "token",
                affiliationName = affiliationName,
                vaultName = vaultName,
                permissions = listOf("permission1")
            )
            assertThat(booberVault).isNotNull()
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/vault/$affiliationName/$vaultName")
        assertThat(requests.last()?.bodyAsObject<BooberVaultInput>()?.permissions).isEqualTo(listOf("permission2"))
    }
}
