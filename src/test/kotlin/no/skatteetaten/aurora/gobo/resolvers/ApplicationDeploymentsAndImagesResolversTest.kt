package no.skatteetaten.aurora.gobo.resolvers

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ApplicationDeploymentsAndImagesResolversTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/queries/getApplicationDeploymentsAndImages.graphql")
    private lateinit var applicationDeploymentsAndImages: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryServiceBlocking

    @BeforeEach
    fun setUp() {
        every { applicationService.getApplicationDeployment(any<String>()) } returns ApplicationDeploymentResourceBuilder().build()
    }

    @Test
    fun `Query for application deployments and images, throw exception for images`() {
        every { imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(any(), any()) } throws
            RuntimeException("test exception")

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
