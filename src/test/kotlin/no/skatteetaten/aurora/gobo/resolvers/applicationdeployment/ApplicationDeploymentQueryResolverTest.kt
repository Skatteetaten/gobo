package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
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

@GraphQLTest
class ApplicationDeploymentQueryResolverTest {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
        every { applicationService.getApplicationDeployment(any()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Query for application deployment`() {
        val variables = mapOf("id" to "123")
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("applicationDeployment") {
                graphqlData("id").isEqualTo("123")
                graphqlData("status.reports").exists()
                graphqlData("status.reasons").exists()
                graphqlData("message").exists()
            }
    }
}
