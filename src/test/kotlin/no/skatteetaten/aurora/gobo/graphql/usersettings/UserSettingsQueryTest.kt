package no.skatteetaten.aurora.gobo.graphql.usersettings

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentFilterResourceBuilder
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsResource
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(UserSettingsQuery::class)
class UserSettingsQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getUserSettingsWithAffiliation.graphql")
    private lateinit var getUserSettingsWithAffiliationQuery: Resource

    @Value("classpath:graphql/queries/getUserSettings.graphql")
    private lateinit var getUserSettingsQuery: Resource

    @MockkBean
    private lateinit var userSettingsService: UserSettingsService

    @BeforeEach
    fun setUp() {
        coEvery { userSettingsService.getUserSettings("test-token") } returns
            UserSettingsResource(
                listOf(
                    ApplicationDeploymentFilterResourceBuilder(affiliation = "aurora").build(),
                    ApplicationDeploymentFilterResourceBuilder(affiliation = "paas").build()
                )
            )
    }

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
            .graphqlDoesNotContainErrors()
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
            .graphqlDoesNotContainErrors()
    }
}
