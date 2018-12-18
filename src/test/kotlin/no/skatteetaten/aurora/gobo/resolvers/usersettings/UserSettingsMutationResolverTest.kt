package no.skatteetaten.aurora.gobo.resolvers.usersettings

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.mockito.BDDMockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class UserSettingsMutationResolverTest {

    @Value("classpath:graphql/updateUserSettings.graphql")
    private lateinit var updateUserSettingsMutation: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @MockBean
    private lateinit var userSettingsService: UserSettingsService

    @BeforeEach
    fun setUp() {
        given(openShiftUserLoader.findOpenShiftUserByToken(anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(openShiftUserLoader, userSettingsService)

    @Test
    fun `Update user settings`() {
        val userSettings = UserSettings(
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
            .jsonPath("$.data.updateUserSettings").isEqualTo(true)

        verify(userSettingsService).updateUserSettings("test-token", userSettings)
    }
}