package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.graphql.GoboInstrumentation
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduleTasks(val goboInstrumentation: GoboInstrumentation) {

    @Scheduled(fixedRateString = "\${gobo.updateFieldUsage.fixedRate:5000}")
    fun insertOrUpdateFieldUsageAsScheduledTask() {
        goboInstrumentation.update()
    }
}
