package no.skatteetaten.aurora.gobo.graphql.vault

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.isFalse
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.BooberVault
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.service.AffiliationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

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

    private val vaultSimple = BooberVault(
        name = "boober",
        hasAccess = true,
        permissions = listOf("APP_PaaS_utv"),
        secrets = mapOf("latest.properties" to "QVRTX1VTRVJOQU1FPWJtYwp")
    )

    private val vaultList = listOf(
        BooberVault(
            name = "boober",
            hasAccess = true,
            permissions = listOf("APP_PaaS_utv"),
            secrets = mapOf("latest.properties" to "QVRTX1VTRVJOQU1FPWJtYwp")
        ),
        BooberVault(
            name = "rosita",
            hasAccess = false,
            permissions = listOf("APP_PaaS_utv", "APP_PaaS_drift"),
            secrets = mapOf("latest.properties" to "RklPTkFfU0VDUkVUX0tFWT1")
        ),
        BooberVault(
            name = "jenkins-gnupg",
            hasAccess = true,
            permissions = listOf("APP_PaaS_utv", "APP_PaaS_drift"),
            secrets = mapOf(
                "pubring-old.gpg" to "mQGiBEE2LvgRBACfDMHe0CHihg9q5pBpdgyNr2xo9J",
                "pubring.gpg" to "mQGNBF2yj5wBDADngY7jzndMFkyoMfzoB2p7ih"
            )
        )
    )

    @Test
    fun `Query for single vault`() {
        coEvery { vaultService.getVault(any()) } returns vaultSimple

        val variables = mapOf(
            "affiliationNames" to listOf("aurora"),
            "vaultNames" to listOf("boober"),
            "secretNames" to listOf("latest.properties")
        )

        webTestClient.queryGraphQL(getVaultsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.vaults[0]") {
                graphqlData("name").isEqualTo("boober")
                graphqlData("hasAccess").isTrue()
                graphqlData("permissions").isEqualTo("APP_PaaS_utv")
                graphqlData("secrets[0].name").isEqualTo("latest.properties")
                graphqlData("secrets[0].base64Content").isEqualTo("QVRTX1VTRVJOQU1FPWJtYwp")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query from list of vaults`() {
        coEvery { vaultService.getVault(any()) } returns vaultList[2]

        val variables =
            mapOf("affiliationNames" to listOf("aurora"), "vaultNames" to listOf("boober", "rosita", "jenkins-gnupg"))
        webTestClient.queryGraphQL(getVaultsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.vaults[0]") {
                graphqlData("name").isEqualTo("jenkins-gnupg")
                graphqlData("hasAccess").isTrue()
                graphqlData("permissions[0]").isEqualTo("APP_PaaS_utv")
                graphqlData("permissions[1]").isEqualTo("APP_PaaS_drift")
                graphqlData("secrets[0].name").isEqualTo("pubring-old.gpg")
                graphqlData("secrets[0].base64Content").isEqualTo("mQGiBEE2LvgRBACfDMHe0CHihg9q5pBpdgyNr2xo9J")
                graphqlData("secrets[1].name").isEqualTo("pubring.gpg")
                graphqlData("secrets[1].base64Content").isEqualTo("mQGNBF2yj5wBDADngY7jzndMFkyoMfzoB2p7ih")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for all vaults using no vault names as input argument`() {
        coEvery { vaultService.getVaults(any(), any()) } returns vaultList

        val variables = mapOf("affiliationNames" to listOf("aurora"))
        webTestClient.queryGraphQL(getVaultsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.vaults[0]") {
                graphqlData("name").isEqualTo("boober")
                graphqlData("hasAccess").isTrue()
                graphqlData("permissions").isEqualTo("APP_PaaS_utv")
                graphqlData("secrets[0].name").isEqualTo("latest.properties")
                graphqlData("secrets[0].base64Content").isEqualTo("QVRTX1VTRVJOQU1FPWJtYwp")
            }
            .graphqlDataWithPrefix("affiliations.edges[0].node.vaults[1]") {
                graphqlData("name").isEqualTo("rosita")
                graphqlData("hasAccess").isFalse()
                graphqlData("permissions[0]").isEqualTo("APP_PaaS_utv")
                graphqlData("permissions[1]").isEqualTo("APP_PaaS_drift")
                graphqlData("secrets[0].name").isEqualTo("latest.properties")
                graphqlData("secrets[0].base64Content").isEqualTo("RklPTkFfU0VDUkVUX0tFWT1")
            }
            .graphqlDataWithPrefix("affiliations.edges[0].node.vaults[2]") {
                graphqlData("name").isEqualTo("jenkins-gnupg")
                graphqlData("hasAccess").isTrue()
                graphqlData("permissions[0]").isEqualTo("APP_PaaS_utv")
                graphqlData("permissions[1]").isEqualTo("APP_PaaS_drift")
                graphqlData("secrets[0].name").isEqualTo("pubring-old.gpg")
                graphqlData("secrets[0].base64Content").isEqualTo("mQGiBEE2LvgRBACfDMHe0CHihg9q5pBpdgyNr2xo9J")
                graphqlData("secrets[1].name").isEqualTo("pubring.gpg")
                graphqlData("secrets[1].base64Content").isEqualTo("mQGNBF2yj5wBDADngY7jzndMFkyoMfzoB2p7ih")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get vaults with partial result`() {
        coEvery { vaultService.getVault(any()) } returns vaultList[0] andThenThrows RuntimeException("test exception")

        val variables =
            mapOf("affiliationNames" to listOf("aurora"), "vaultNames" to listOf("boober", "non-existing-vault"))

        webTestClient.queryGraphQL(queryResource = getVaultsQuery, variables = variables, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.edges[0].node.vaults.length()").isEqualTo(1)
            .graphqlErrorsFirst("message").isNotEmpty
    }
}
