package no.skatteetaten.aurora.gobo.resolvers.userSettings

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class UserSettingsTest {

    private val userSettings = UserSettings(
        listOf(
            ApplicationDeploymentFilter("filter1", "aurora"),
            ApplicationDeploymentFilter("filter2", "paas")
        )
    )

    @Test
    fun `Get all applicationDeploymentFilters`() {
        val filters = userSettings.applicationDeploymentFilters()
        assert(filters).hasSize(2)
        assert(filters[0].affiliation).isEqualTo("aurora")
        assert(filters[1].affiliation).isEqualTo("paas")
    }

    @Test
    fun `Get applicationDeploymentFilters for affiliation`() {
        val filters = userSettings.applicationDeploymentFilters(listOf("aurora"))
        assert(filters).hasSize(1)
        assert(filters[0].affiliation).isEqualTo("aurora")
    }
}