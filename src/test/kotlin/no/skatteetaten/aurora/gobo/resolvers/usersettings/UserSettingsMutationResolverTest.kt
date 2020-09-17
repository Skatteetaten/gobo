package no.skatteetaten.aurora.gobo.resolvers.usersettings

// import com.fasterxml.jackson.module.kotlin.convertValue
// import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
// import io.mockk.verify
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
// import no.skatteetaten.aurora.gobo.resolvers.graphqlData
// import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
// import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Disabled
// import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled
class UserSettingsMutationResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/updateUserSettings.graphql")
    private lateinit var updateUserSettingsMutation: Resource

    @MockkBean(relaxed = true)
    private lateinit var userSettingsService: UserSettingsService

/*
    @Test
    fun `Update user settings`() {
        val userSettings = UserSettingsInput(
            listOf(
                ApplicationDeploymentFilter(
                    name = "filter",
                    default = true,
                    affiliation = "paas",
                    applications = listOf("app1", "app2"),
                    environments = listOf("env1", "env2")
                )
            )
        )
        webTestClient.queryGraphQL(
            queryResource = updateUserSettingsMutation,
            variables = mapOf("input" to jacksonObjectMapper().convertValue<Map<String, Any>>(userSettings)),
            token = "test-token"
        ).expectStatus().isOk
            .expectBody()
            .graphqlData("updateUserSettings").isEqualTo(true)
            .graphqlDoesNotContainErrors()

        verify { userSettingsService.updateUserSettings("test-token", userSettings) }
    }
*/
}
