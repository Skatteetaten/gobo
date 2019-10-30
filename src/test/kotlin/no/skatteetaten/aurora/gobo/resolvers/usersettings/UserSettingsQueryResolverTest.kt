package no.skatteetaten.aurora.gobo.resolvers.usersettings

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentFilterResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsResource
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
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
class UserSettingsQueryResolverTest {

    @Value("classpath:graphql/queries/getUserSettingsWithAffiliation.graphql")
    private lateinit var getUserSettingsWithAffiliationQuery: Resource

    @Value("classpath:graphql/queries/getUserSettings.graphql")
    private lateinit var getUserSettingsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var userSettingsService: UserSettingsService

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        every { userSettingsService.getUserSettings("test-token") } returns
            UserSettingsResource(
                listOf(
                    ApplicationDeploymentFilterResourceBuilder(affiliation = "aurora").build(),
                    ApplicationDeploymentFilterResourceBuilder(affiliation = "paas").build()
                )
            )

        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Query for application deployment filters`() {
        webTestClient
            .queryGraphQL(
                queryResource = getUserSettingsQuery,
                token = "test-token"
            )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("userSettings.applicationDeploymentFilters") {
                graphqlData("length()").isEqualTo(2)
                graphqlData("[0].affiliation").isEqualTo("aurora")
                graphqlData("[1].affiliation").isEqualTo("paas")
            }
    }

    @Test
    fun `Query for application deployment filters given affiliation`() {
        webTestClient
            .queryGraphQL(
                queryResource = getUserSettingsWithAffiliationQuery,
                variables = mapOf("affiliations" to listOf("aurora")),
                token = "test-token"
            )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("userSettings.applicationDeploymentFilters") {
                graphqlData("length()").isEqualTo(1)
                graphqlDataFirst("name").isNotEmpty
                graphqlDataFirst("default").isBoolean
                graphqlDataFirst("affiliation").isEqualTo("aurora")
                graphqlDataFirst("applications").isNotEmpty
                graphqlDataFirst("environments").isNotEmpty
            }
    }
}
