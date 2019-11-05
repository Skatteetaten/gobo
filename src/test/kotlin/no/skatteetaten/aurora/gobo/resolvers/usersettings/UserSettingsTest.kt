package no.skatteetaten.aurora.gobo.resolvers.usersettings

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class UserSettingsTest {

    private val userSettings = UserSettings(
        listOf(
            ApplicationDeploymentFilter("filter1", "aurora"),
            ApplicationDeploymentFilter.defaultApplicationDeploymentFilter("filter2", "paas")
        )
    )

    @Test
    fun `Get all applicationDeploymentFilters`() {
        val filters = userSettings.applicationDeploymentFilters()
        assertThat(filters).hasSize(2)
        assertThat(filters[0].affiliation).isEqualTo("aurora")
        assertThat(filters[1].affiliation).isEqualTo("paas")
    }

    @Test
    fun `Get applicationDeploymentFilters for affiliation`() {
        val filters = userSettings.applicationDeploymentFilters(listOf("aurora"))
        assertThat(filters).hasSize(1)
        assertThat(filters[0].affiliation).isEqualTo("aurora")
    }
}
