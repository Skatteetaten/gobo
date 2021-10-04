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
    fun `Create environment status for ApplicationDeployment with observe status`() {
        val ad = ApplicationDeploymentResourceBuilder(status = "OBSERVE").build()
        val status = EnvironmentStatus.create(ad)
        assertThat(status.state).isEqualTo(EnvironmentStatusType.COMPLETED)
    }

    @Test
    fun `Create environment status for ApplicationDeployment with off status`() {
        val ad = ApplicationDeploymentResourceBuilder(status = "OFF").build()
        val status = EnvironmentStatus.create(ad)
        assertThat(status.state).isEqualTo(EnvironmentStatusType.INACTIVE)
    }

    @Test
    fun `Create environment status for ApplicationDeployment with health status`() {
        val ad = ApplicationDeploymentResourceBuilder().build()
        val status = EnvironmentStatus.create(ad)
        assertThat(status.state).isEqualTo(EnvironmentStatusType.COMPLETED)
    }

    @Test
    fun `Create environment status for ApplicationDeployment with unknown status`() {
        val ad = ApplicationDeploymentResourceBuilder(status = "SOME_BOGUS_STATE").build()
        val status = EnvironmentStatus.create(ad)
        assertThat(status.state).isEqualTo(EnvironmentStatusType.FAILED)
    }
}
