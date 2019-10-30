package no.skatteetaten.aurora.gobo.resolvers.application

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.kotlin.core.publisher.toMono

@GraphQLTest
class ApplicationQueryResolverTest {

    @Value("classpath:graphql/queries/getApplications.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var applicationServiceBlocking: ApplicationServiceBlocking

    @MockkBean
    private lateinit var permissionService: PermissionService

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Query for applications given affiliations`() {
        val affiliations = listOf("paas")
        every { applicationServiceBlocking.getApplications(affiliations) } returns listOf(ApplicationResourceBuilder().build())
        every { permissionService.getPermission(any(), any()) } returns AuroraNamespacePermissions(
            view = true,
            admin = true,
            namespace = "namespace"
        ).toMono()
        every { applicationServiceBlocking.getApplicationDeploymentDetails(any(), any()) } returns ApplicationDeploymentDetailsBuilder().build()

        val variables = mapOf("affiliations" to affiliations)
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("applications.totalCount").isNumber
            .graphqlDataWithPrefix("applications.edges[0].node") {
                graphqlData("applicationDeployments[0].affiliation.name").isNotEmpty
                graphqlData("applicationDeployments[0].namespace.name").isNotEmpty
                graphqlData("applicationDeployments[0].namespace.permission.paas.admin").isNotEmpty
                graphqlData("applicationDeployments[0].details.updatedBy").isNotEmpty
                graphqlData("applicationDeployments[0].details.buildTime").isNotEmpty
                graphqlData("applicationDeployments[0].details.deployDetails.paused").isEqualTo(false)
                graphqlData("imageRepository.repository").doesNotExist()
            }
    }
}
