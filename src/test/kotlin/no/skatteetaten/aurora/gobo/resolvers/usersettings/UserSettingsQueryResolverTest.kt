package no.skatteetaten.aurora.gobo.resolvers.usersettings

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
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
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

    @MockBean
    private lateinit var userSettingsService: UserSettingsService

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        given(userSettingsService.getUserSettings("test-token")).willReturn(
            UserSettingsResource(
                listOf(
                    ApplicationDeploymentFilterResourceBuilder(affiliation = "aurora").build(),
                    ApplicationDeploymentFilterResourceBuilder(affiliation = "paas").build()
                )
            )
        )

        given(openShiftUserLoader.findOpenShiftUserByToken(anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(userSettingsService, openShiftUserLoader)

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
                it.graphqlData("length()").isEqualTo(2)
                it.graphqlData("[0].affiliation").isEqualTo("aurora")
                it.graphqlData("[1].affiliation").isEqualTo("paas")
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
                it.graphqlData("length()").isEqualTo(1)
                it.graphqlDataFirst("name").isNotEmpty
                it.graphqlDataFirst("default").isBoolean
                it.graphqlDataFirst("affiliation").isEqualTo("aurora")
                it.graphqlDataFirst("applications").isNotEmpty
                it.graphqlDataFirst("environments").isNotEmpty
            }
    }
}