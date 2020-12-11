package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.graphql.GoboInstrumentation
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled

@Configuration
class GoboUsageConfig(private val goboInstrumentation: GoboInstrumentation) {
    /**
     * Updates the usage data every 10 minutes by default
     */
    @Scheduled(fixedRateString = "\${gobo.graphqlUsage.fixedRate:600000}")
    fun updateGraphqlUsage() {
        goboInstrumentation.update()
    }
}
