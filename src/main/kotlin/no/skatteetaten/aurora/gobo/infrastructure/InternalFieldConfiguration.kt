package no.skatteetaten.aurora.gobo.infrastructure

import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldRepository
import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackageClasses = [FieldRepository::class])
@EntityScan(basePackageClasses = [FieldEnity::class])
@EnableTransactionManagement
@EnableScheduling
internal class InternalFieldConfiguration
