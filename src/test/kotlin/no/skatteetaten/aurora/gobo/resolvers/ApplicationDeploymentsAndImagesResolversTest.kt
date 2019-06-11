package no.skatteetaten.aurora.gobo.resolvers

import com.nhaarman.mockito_kotlin.any
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class ApplicationDeploymentsAndImagesResolversTest {
    @Value("classpath:graphql/queries/getApplicationDeploymentsAndImages.graphql")
    private lateinit var applicationDeploymentsAndImages: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @MockBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockBean
    private lateinit var imageRegistryService: ImageRegistryServiceBlocking

    @BeforeEach
    fun setUp() {
        given(openShiftUserLoader.findOpenShiftUserByToken(any()))
            .willReturn(OpenShiftUserBuilder().build())

        given(applicationService.getApplicationDeployment(any()))
            .willReturn(ApplicationDeploymentResourceBuilder().build())
    }

    @Test
    fun `Query for application deployments and images, throw exception for images`() {
        given(imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(any(), any()))
            .willThrow(RuntimeException("test exception"))

        webTestClient.queryGraphQL(
            queryResource = applicationDeploymentsAndImages,
            variables = mapOf("id" to "123", "repository" to "docker-registry/skatteetaten/123"),
            token = "test-token"
        ).expectStatus().isOk
            .expectBody()
            .graphqlData("applicationDeployment.affiliation.name").isEqualTo("paas")
            .graphqlErrors("length()").isEqualTo(1)
    }
}