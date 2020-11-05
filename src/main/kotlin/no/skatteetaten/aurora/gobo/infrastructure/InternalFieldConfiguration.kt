package no.skatteetaten.aurora.gobo.infrastructure

import no.skatteetaten.aurora.gobo.domain.FieldRepository
import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackageClasses = arrayOf(FieldRepository::class))
@EntityScan(basePackageClasses = arrayOf(FieldEnity::class))
@EnableTransactionManagement
internal class InternalFieldConfiguration
