package no.skatteetaten.aurora.gobo.resolvers.userSettings

import no.skatteetaten.aurora.gobo.ApplicationDeploymentFilterResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentFilterService
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class UserSettingsQueryResolverTest {

    private val filters = "\$.data.userSettings.applicationDeploymentFilters"

    @Value("classpath:graphql/getUserSettingsWithAffiliation.graphql")
    private lateinit var getUserSettingsWithAffiliationQuery: Resource

    @Value("classpath:graphql/getUserSettings.graphql")
    private lateinit var getUserSettingsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationDeploymentFilterService: ApplicationDeploymentFilterService

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        given(applicationDeploymentFilterService.getFilters("test-token")).willReturn(
            listOf(
                ApplicationDeploymentFilterResourceBuilder(affiliation = "aurora").build(),
                ApplicationDeploymentFilterResourceBuilder(affiliation = "paas").build()
            )
        )

        given(openShiftUserLoader.findOpenShiftUserByToken(BDDMockito.anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(applicationDeploymentFilterService, openShiftUserLoader)

    @Test
    fun `Query for application deployment filters`() {
        webTestClient
            .queryGraphQL(
                queryResource = getUserSettingsQuery,
                token = "test-token"
            )
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$filters.length()").isEqualTo(2)
            .jsonPath("$filters[0].affiliation").isEqualTo("aurora")
            .jsonPath("$filters[1].affiliation").isEqualTo("paas")
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
            .jsonPath("$filters.length()").isEqualTo(1)
            .jsonPath("$filters[0].name").isNotEmpty
            .jsonPath("$filters[0].affiliation").isEqualTo("aurora")
            .jsonPath("$filters[0].applications").isNotEmpty
            .jsonPath("$filters[0].environments").isNotEmpty
    }
}