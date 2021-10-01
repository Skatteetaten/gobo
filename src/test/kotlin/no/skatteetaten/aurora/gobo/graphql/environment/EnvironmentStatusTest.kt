package no.skatteetaten.aurora.gobo.graphql.environment

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import org.junit.jupiter.api.Test

class EnvironmentStatusTest {
    @Test
    fun `Create environment status for ApplicationDeployment with failed status`() {
        val ad = ApplicationDeploymentResourceBuilder(status = "DOWN").build()
        val status = EnvironmentStatus.create(ad)
        assertThat(status.state).isEqualTo(EnvironmentStatusType.FAILED)
    }

    @Test
    fun `Create environment status for ApplicationDeployment with health status`() {
        val ad = ApplicationDeploymentResourceBuilder().build()
        val status = EnvironmentStatus.create(ad)
        assertThat(status.state).isEqualTo(EnvironmentStatusType.COMPLETED)
    }
}
