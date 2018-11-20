package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.toMono

@GraphQLTest
class ApplicationQueryResolverTest {
    private val firstApplicationDeployment = "\$.data.applications.edges[0].node.applicationDeployments[0]"

    @Value("classpath:graphql/getApplications.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationServiceBlocking: ApplicationServiceBlocking

    @MockBean
    private lateinit var permissionService: PermissionService
    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        given(openShiftUserLoader.findOpenShiftUserByToken(anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(applicationServiceBlocking, openShiftUserLoader, permissionService)

    @Test
    fun `Query for applications given affiliations`() {
        val affiliations = listOf("paas")
        given(applicationServiceBlocking.getApplications(affiliations))
            .willReturn(listOf(ApplicationResourceBuilder().build()))

        given(permissionService.getPermission(ArgumentMatchers.anyString())).willReturn(
            AuroraNamespacePermissions(
                true,
                true,
                "namespace"
            ).toMono()
        )

        given(applicationServiceBlocking.getApplicationDeploymentDetails(anyString(), anyString()))
            .willReturn(ApplicationDeploymentDetailsBuilder().build())

        val variables = mapOf("affiliations" to affiliations)
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.applications.totalCount").isNumber
            .jsonPath("$firstApplicationDeployment.affiliation.name").isNotEmpty
            .jsonPath("$firstApplicationDeployment.namespace.name").isNotEmpty
            .jsonPath("$firstApplicationDeployment.namespace.permission.paas.admin").isNotEmpty
            .jsonPath("$firstApplicationDeployment.details.buildTime").isNotEmpty
    }
}