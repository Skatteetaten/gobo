package no.skatteetaten.aurora.gobo

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboInstrumentation
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled

private val logger = KotlinLogging.logger {}

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

@Profile("local")
@Configuration
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
class GoboInMemoryUsageConfig {
    init {
        logger.info("Starting gobo with in-memory usage implementations, no db integration")
    }
}

@Profile("!local")
@Configuration
class GoboDatabaseConfig {
    init {
        logger.info("Starting gobo with database usage implementations")
    }
}
