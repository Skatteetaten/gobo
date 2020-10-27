package no.skatteetaten.aurora.gobo.graphql.permission

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.namespace.Namespace
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import org.springframework.stereotype.Component

@Component
class PermissionDataLoader(
    val permissionService: PermissionService
) : KeyDataLoader<Namespace, Permission> {

    override suspend fun getByKey(key: Namespace, context: GoboGraphQLContext): Permission {
        return permissionService.getPermission(key.name, context.token()).let {
            Permission(paas = PermissionDetails(view = it.view, admin = it.admin))
        }
    }
}
