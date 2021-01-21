package no.skatteetaten.aurora.gobo.integration.boober

import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.graphql.vault.Vault
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer

internal class VaultServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val vaultService =
        VaultService(BooberWebClient(url.toString(), WebClient.create(), testObjectMapper()))

    private val vaultSimple = Vault(
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
            assertThat(vault.permissions[0]).isEqualTo("APP_PaaS_utv")
            val secret = vault.secrets.entries
            assertThat(vault.secrets[0].).isEqualTo("APP_PaaS_utv")
        }
    }
}
