package no.skatteetaten.aurora.gobo.resolvers.userSettings

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class UserSettingsTest {

    @Test
    fun `Get applicationDeploymentFilters for affiliation`() {
        val userSettings = UserSettings(listOf(ApplicationDeploymentFilter("aurora"), ApplicationDeploymentFilter("paas")))
        val filters = userSettings.applicationDeploymentFilters("aurora")
        assert(filters).hasSize(1)
        assert(filters[0].affiliation).isEqualTo("aurora")
    }
}