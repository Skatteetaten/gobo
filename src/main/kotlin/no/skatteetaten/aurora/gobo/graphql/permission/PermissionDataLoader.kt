package no.skatteetaten.aurora.gobo.graphql.permission

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.namespace.Namespace
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class PermissionDataLoader(
    val permissionService: PermissionService
) : KeyDataLoader<Namespace, Permission> {

    override suspend fun getByKey(key: Namespace, context: GoboGraphQLContext): Permission {
        return runCatching {
            permissionService.getPermission(key.name, context.token()).let {
                Permission(paas = PermissionDetails(view = it.view, admin = it.admin))
            }
        }.recoverCatching {
            logger.warn("Failed checking for permission message=${it.localizedMessage}")
            Permission()
        }.getOrThrow()
    }
}
