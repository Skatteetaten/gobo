package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    @Value("classpath:graphql/queries/getApplications.graphql")
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

        given(permissionService.getPermission(anyString(), anyString())).willReturn(
            AuroraNamespacePermissions(
                view = true,
                admin = true,
                namespace = "namespace"
            ).toMono()
        )

        given(applicationServiceBlocking.getApplicationDeploymentDetails(anyString(), anyString()))
            .willReturn(ApplicationDeploymentDetailsBuilder().build())

        val variables = mapOf("affiliations" to affiliations)
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("applications.totalCount").isNumber
            .graphqlDataWithPrefix("applications.edges[0].node") {
                it.graphqlData("applicationDeployments[0].affiliation.name").isNotEmpty
                it.graphqlData("applicationDeployments[0].namespace.name").isNotEmpty
                it.graphqlData("applicationDeployments[0].namespace.permission.paas.admin").isNotEmpty
                it.graphqlData("applicationDeployments[0].details.buildTime").isNotEmpty
                it.graphqlData("applicationDeployments[0].details.deployDetails.paused").isEqualTo(false)
                it.graphqlData("imageRepository.repository").doesNotExist()
            }
    }
}