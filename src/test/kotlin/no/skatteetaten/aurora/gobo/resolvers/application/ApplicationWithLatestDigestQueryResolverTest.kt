package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
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
class ApplicationWithLatestDigestQueryResolverTest {
    private val imageDetails =
        "applications.edges[0].node.applicationDeployments[0].details.imageDetails"

    @Value("classpath:graphql/queries/getApplicationsWithLatestDigest.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationServiceBlocking: ApplicationServiceBlocking

    @MockBean
    private lateinit var imageRegistryServiceBlocking: ImageRegistryServiceBlocking

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
    fun tearDown() =
        reset(applicationServiceBlocking, openShiftUserLoader, permissionService, imageRegistryServiceBlocking)

    @Test
    fun `Query for latest image from repo`() {
        val affiliations = listOf("paas")

        val details = ApplicationDeploymentDetailsBuilder().build()

        val tag = ImageTag.fromTagString(details.imageDetails!!.dockerImageTagReference!!)

        given(imageRegistryServiceBlocking.resolveTagToSha(tag)).willReturn("sha256:123")

        given(applicationServiceBlocking.getApplications(affiliations))
            .willReturn(listOf(ApplicationResourceBuilder().build()))

        given(permissionService.getPermission(anyString(), anyString()))
            .willReturn(AuroraNamespacePermissions(true, true, "namespace").toMono())

        given(applicationServiceBlocking.getApplicationDeploymentDetails(anyString(), anyString()))
            .willReturn(details)

        val variables = mapOf("affiliations" to affiliations)
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("applications.totalCount").isNumber
            .graphqlDataWithPrefix("applications.edges[0].node.applicationDeployments[0].details.imageDetails") {
                it.graphqlData("dockerImageTagReference").isEqualTo("docker.registry/group/name:2")
                it.graphqlData("digest").isEqualTo("sha256:123")
                it.graphqlData("isLatestDigest").isEqualTo(true)
            }
    }
}