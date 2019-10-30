package no.skatteetaten.aurora.gobo.resolvers.usersettings

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
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
class UserSettingsMutationResolverTest {

    @Value("classpath:graphql/mutations/updateUserSettings.graphql")
    private lateinit var updateUserSettingsMutation: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @MockkBean(relaxed = true)
    private lateinit var userSettingsService: UserSettingsService

    @BeforeEach
    fun setUp() {
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

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
            .graphqlData("updateUserSettings").isEqualTo(true)

        verify { userSettingsService.updateUserSettings("test-token", userSettings) }
    }
}
