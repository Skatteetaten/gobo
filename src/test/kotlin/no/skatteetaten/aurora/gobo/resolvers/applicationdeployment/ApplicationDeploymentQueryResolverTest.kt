package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class ApplicationDeploymentQueryResolverTest {

    @Value("classpath:graphql/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        given(openShiftUserLoader.findOpenShiftUserByToken(anyString()))
            .willReturn(OpenShiftUserBuilder().build())
        given(applicationService.getApplicationDeployment(anyString())).willReturn(
            ApplicationDeploymentResourceBuilder(id = "123", msg = "Hei").build()
        )
    }

    @AfterEach
    fun tearDown() = reset(openShiftUserLoader, applicationService)

    @Test
    fun `Query for application deployment`() {
        val variables = mapOf("id" to "123")
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.applicationDeployment.id").isEqualTo("123")
            .jsonPath("$.data.applicationDeployment.status.reports").exists()
            .jsonPath("$.data.applicationDeployment.status.reasons").exists()
            .jsonPath("$.data.applicationDeployment.message").exists()
    }
}