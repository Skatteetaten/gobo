package no.skatteetaten.aurora.gobo.infrastructure

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import java.lang.annotation.Documented

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Documented
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('\${spring.datasource.url:}')")
annotation class ConditionalOnDatabaseUrl

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Documented
@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${spring.datasource.url:}')")
annotation class ConditionalOnMissingDatabaseUrl
