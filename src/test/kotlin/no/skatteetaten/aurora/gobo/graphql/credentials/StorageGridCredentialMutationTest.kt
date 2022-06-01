package no.skatteetaten.aurora.gobo.graphql.credentials

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.slot
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.contains
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirstContainsMessage
import no.skatteetaten.aurora.gobo.graphql.isFalse
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerResult
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerServiceDisabled
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerServiceReactive
import no.skatteetaten.aurora.gobo.integration.herkimer.RegisterResourceAndClaimCommand
import no.skatteetaten.aurora.gobo.integration.naghub.DetailedMessage
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubColor
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubResult
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource

class StorageGridCredentialMutationTest {

    @WithMockUser(authorities = ["testAdGroup"])
    @Nested
    inner class AuthorizedTokenStorageGridCredentialMutation(
        @Value("\${openshift.cluster}") val cluster: String
    ) : StorageGridCredentialMutationBaseTest() {
        @MockkBean
        private lateinit var herkimerService: HerkimerService

        @Test
        fun `Mutate SG credentials return false with message given response false`() {
            coEvery { herkimerService.registerResourceAndClaim(any()) } returns HerkimerResult(false)

            webTestClient.queryGraphQL(
                queryResource = registerStorageGridTenantMutation,
                variables = registerStorageGridVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlDataWithPrefix("registerStorageGridTenant") {
                    graphqlData("success").isFalse()
                    graphqlData("message").contains("tenantName=bg-$cluster could not be registered")
                }

            val message = messageSlot.captured.first()
            assertThat(message.text).contains("needs to be manually registered")
            assertThat(message.color).isEqualTo(NagHubColor.Red)
        }

        @Test
        fun `Mutate SG credentials return true given response success`() {

            val instanceNameSlot = slot<RegisterResourceAndClaimCommand>()
            coEvery {
                herkimerService.registerResourceAndClaim(capture(instanceNameSlot))
            } returns HerkimerResult(true)

            val expectedTenantName = "bg-$cluster"

            webTestClient.queryGraphQL(
                queryResource = registerStorageGridTenantMutation,
                variables = registerStorageGridVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlDataWithPrefix("registerStorageGridTenant") {
                    graphqlData("success").isTrue()
                    graphqlData("message").contains("tenantName=$expectedTenantName has been successfully registered")
                }
                .graphqlDoesNotContainErrors()

            assertThat(instanceNameSlot.captured.resourceName).isEqualTo(expectedTenantName)
            val message = messageSlot.captured.first()
            assertThat(message.text).contains("StorageGrid tenant tenantName=$expectedTenantName has been successfully registered")
            assertThat(message.color).isEqualTo(NagHubColor.Yellow)
        }
    }

    @Nested
    inner class UnauthorizedTokenStorageGridCredentialMutation : StorageGridCredentialMutationBaseTest() {
        @MockkBean
        private lateinit var herkimerService: HerkimerService

        @Test
        fun `Error when unauthorized and registering SG credentials`() {
            webTestClient.queryGraphQL(
                queryResource = registerStorageGridTenantMutation,
                variables = registerStorageGridVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Access denied, missing/invalid token or the token does not have the required permissions")
        }
    }

    @WithMockUser(authorities = ["wrongAdGroup"])
    @Nested
    inner class WrongAdGroupTokenStorageGridCredentialMutation : StorageGridCredentialMutationBaseTest() {
        @MockkBean(relaxed = true)
        private lateinit var herkimerService: HerkimerService

        @Test
        fun `Error when wrong ad group`() {
            webTestClient.queryGraphQL(
                queryResource = registerStorageGridTenantMutation,
                variables = registerStorageGridVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Access denied, missing/invalid token or the token does not have the required permissions")
        }
    }

    @WithMockUser(authorities = ["testAdGroup"])
    @TestPropertySource(properties = ["integrations.herkimer.url=false"])
    @Nested
    @Import(HerkimerServiceReactive::class, HerkimerServiceDisabled::class)
    inner class UnavailableHerkimerStorageGridCredentialMutation : StorageGridCredentialMutationBaseTest() {

        @Test
        fun `verify herkimer is disabled when no url is set`() {
            webTestClient.queryGraphQL(
                queryResource = registerStorageGridTenantMutation,
                variables = registerStorageGridVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Herkimer integration is disabled")
        }
    }

    @WithMockUser(authorities = ["testAdGroup"])
    @TestPropertySource(properties = ["integrations.storagegrid.operator.application.deployment.id=false"])
    @Nested
    inner class StorageGridOperatorApplicationDeploymentIdNotSetCredentialMutation : StorageGridCredentialMutationBaseTest() {

        @Test
        fun `verify mutation is disabled when StorageGrid is not present`() {
            webTestClient.queryGraphQL(
                queryResource = registerStorageGridTenantMutation,
                variables = registerStorageGridVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Unknown type StorageGridTenantInput")
        }
    }
}

@Import(StorageGridCredentialMutation::class)
open class StorageGridCredentialMutationBaseTest : GraphQLTestWithoutDbhAndSkap() {
    @Value("classpath:graphql/mutations/registerStorageGridTenant.graphql")
    lateinit var registerStorageGridTenantMutation: Resource

    val storageGridTenantInput = StorageGridTenantInput("84848484", "testadminuser", "password", "bg")
    val registerStorageGridVariables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(storageGridTenantInput)
    )

    val messageSlot = slot<List<DetailedMessage>>()

    @MockkBean
    lateinit var naghubService: NagHubService

    @BeforeEach
    fun setup() {
        coEvery {
            naghubService.sendMessage(any(), capture(messageSlot), any())
        } returns NagHubResult(true)
    }
}
